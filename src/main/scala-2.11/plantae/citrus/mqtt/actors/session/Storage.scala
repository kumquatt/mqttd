package plantae.citrus.mqtt.actors.session

import java.io._
import java.text.SimpleDateFormat
import java.util.{Date, UUID}

import akka.actor.ActorRef
import com.google.common.base.Throwables
import org.slf4j.LoggerFactory
import plantae.citrus.mqtt.actors.SystemRoot
import plantae.citrus.mqtt.packet.{FixedHeader, PublishPacket}
import scodec.bits.ByteVector

class Storage(sessionName: String) extends Serializable {
  private val log = LoggerFactory.getLogger(getClass() + sessionName)
  private val chunkSize = 200

  sealed trait Location

  case object OnMemory extends Location

  case class OnDisk(location: String) extends Location

  case class ReadyMessage(payload: Array[Byte], qos: Short, retain: Boolean, topic: String)

  case class ChunkMessage(var location: Location, var readyMessages: List[ReadyMessage]) {
    def serialize = {
      try {
        val directory = new File(SystemRoot.config.getString("mqtt.broker.session.storeDir") + "/" + sessionName + "/" + (new SimpleDateFormat("yyyy/MM/dd").format(new Date())))
        directory.mkdirs()
        val path: String = directory.getAbsolutePath + "/" + UUID.randomUUID().toString
        val outputStreamer = new ObjectOutputStream(new FileOutputStream(path))
        outputStreamer.writeObject(this)
        outputStreamer.close()
        location = OnDisk(path)
        readyMessages = List()
      } catch {
        case t: Throwable => location = OnMemory
          log.error(" Chunk serialize error : {} ", Throwables.getStackTraceAsString(t))
      }
    }

    def deserialize = {
      location match {
        case OnMemory =>
        case OnDisk(path) =>
          try {
            val inputStreamer = new ObjectInputStream(new FileInputStream(path))
            readyMessages = inputStreamer.readObject().asInstanceOf[ChunkMessage].readyMessages
            location = OnMemory
            new File(path).delete()
            inputStreamer.close()
          } catch {
            case t: Throwable => location = OnMemory
              log.error(" Chunk deserialize error : {} ", Throwables.getStackTraceAsString(t))
          }
      }

    }

    def clear = {
      location match {
        case OnMemory => readyMessages = List()
        case OnDisk(path) => new File(path).delete()
      }
    }
  }


  private var packetIdGenerator: Int = 0
  private var topics: List[String] = List()
  private var readyQueue: List[ChunkMessage] = List()
  private var workQueue: List[PublishPacket] = List()
  private var redoQueue: List[PublishPacket] = List()

  def persist(payload: Array[Byte], qos: Short, retain: Boolean, topic: String) = {
    readyQueue match {
      case head :: rest => {
        if (readyQueue.last.readyMessages.size >= chunkSize) {
          if (readyQueue.last != readyQueue.head) {
            readyQueue.last.serialize
          }
          readyQueue = readyQueue :+ ChunkMessage(OnMemory, List(ReadyMessage(payload, qos, retain, topic)))
        } else
          readyQueue.last.readyMessages = (readyQueue.last.readyMessages :+ ReadyMessage(payload, qos, retain, topic))
      }
      case List() => readyQueue = readyQueue :+ ChunkMessage(OnMemory, List(ReadyMessage(payload, qos, retain, topic)))
    }
  }


  def complete(packetId: Option[Int]) =
    packetId match {
      case Some(y) => workQueue = workQueue.filterNot(_.packetId match {
        case Some(x) => x == y
        case None => false
      })
      case None =>
    }

  def popFirstMessage: Option[ReadyMessage] = {
    readyQueue match {
      case headChunk :: tailChunk =>
        headChunk.deserialize
        headChunk.readyMessages match {
          case headMessage :: tailMessage =>
            headChunk.readyMessages = tailMessage
            Some(headMessage)
          case List() =>
            readyQueue = tailChunk
            popFirstMessage
        }

      case List() => None
    }
  }

  def nextMessage: Option[PublishPacket] = {
    if (workQueue.size > 10) {
      None
    } else {
      redoQueue match {
        case head :: tail =>
          redoQueue = tail
          workQueue = workQueue :+ head
          Some(PublishPacket(FixedHeader(true, head.fixedHeader.qos, head.fixedHeader.retain), head.topic, head.packetId, head.payload))
        case Nil =>
          popFirstMessage match {
            case Some(message) =>
              val publish = message.qos match {
                case x if (x > 0) =>
                  val publishPacket = PublishPacket(FixedHeader(dup = false, qos = message.qos, retain = message.retain),
                    topic = message.topic,
                    packetId = Some(nextPacketId),
                    ByteVector(message.payload)
                  )
                  workQueue = workQueue :+ publishPacket
                  publishPacket

                case x if (x == 0) =>
                  PublishPacket(FixedHeader(dup = false, qos = message.qos, retain = message.retain),
                    topic = message.topic,
                    packetId = Some(nextPacketId),
                    ByteVector(message.payload)
                  )
              }

              Some(publish)
            case None => None
          }
      }

    }
  }

  def socketClose = {
    redoQueue = redoQueue ++ workQueue
    workQueue = Nil
  }

  def clear = {
    topics = List()
    readyQueue.foreach(chunk => chunk.clear)
    readyQueue = List()
    workQueue = List()
  }

  private def nextPacketId: Int = {
    packetIdGenerator = {
      if ((packetIdGenerator + 1) >= Short.MaxValue)
        1
      else (packetIdGenerator + 1)
    }
    packetIdGenerator
  }

  def messageSize = {
    readyQueue.foldLeft(0)((a, b) => a + (b.location match {
      case OnMemory => b.readyMessages.size
      case x: OnDisk => chunkSize
    }))
  }
}

object Storage {
  def apply(sessionName: String) = new Storage(sessionName)

  def apply(session: ActorRef) = new Storage(session.path.name)
}