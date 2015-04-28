package plantae.citrus.mqtt.actors.session

import java.io._
import java.util.UUID

import plantae.citrus.mqtt.dto.publish.PUBLISH
import plantae.citrus.mqtt.dto.{INT, PUBLISHPAYLOAD, STRING}

class Storage(sessionName: String) extends Serializable {

  sealed trait Location

  case object OnMemory extends Location

  case class OnDisk(location: String) extends Location

  case class ReadyMessage(payload: Array[Byte], qos: Short, retain: Boolean, topic: String)

  case class ChunkMessage(var location: Location, var readyMessages: List[ReadyMessage]) {
    def serialize = {
      new File("data/" + sessionName).mkdir()
      val name = "data/" + sessionName + "/" + UUID.randomUUID().toString
      val outputStreamer = new ObjectOutputStream(new FileOutputStream(name))
      outputStreamer.writeObject(this)
      outputStreamer.close()
      location = OnDisk(name)
      readyMessages = List()
    }

    def deserialize = {
      location match {
        case OnMemory =>
        case OnDisk(path) =>
          val inputStreamer = new ObjectInputStream(new FileInputStream(path))
          readyMessages = inputStreamer.readObject().asInstanceOf[ChunkMessage].readyMessages
          location = OnMemory
          new File(path).delete()
          inputStreamer.close()
      }

    }

    def clear = {
      location match {
        case OnMemory => readyMessages = List()
        case OnDisk(path) => new File(path).delete()
      }
    }
  }


  private var packetIdGenerator: Short = 0
  private var topics: List[String] = List()
  private var readyQueue: List[ChunkMessage] = List()
  private var workQueue: List[PUBLISH] = List()

  def persist(payload: Array[Byte], qos: Short, retain: Boolean, topic: String) = {
    readyQueue match {
      case head :: rest => {
        if (readyQueue.last.readyMessages.size >= 50) {
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


  def complete(packetId: Option[Short]) =
    packetId match {
      case Some(y) => workQueue = workQueue.filterNot(_.packetId match {
        case Some(x) => x.value == y
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

  def nextMessage: Option[PUBLISH] = {

    if (workQueue.size > 0) {
      None
    } else popFirstMessage match {
      case Some(message) =>
        val publish = message.qos match {
          case x if (x > 0) =>
            val publish = PUBLISH(false, INT(message.qos), message.retain, STRING(message.topic), Some(INT(nextPacketId)), PUBLISHPAYLOAD(message.payload))
            workQueue = workQueue :+ publish
            publish

          case x if (x == 0) =>
            PUBLISH(false, INT(message.qos), message.retain, STRING(message.topic), None, PUBLISHPAYLOAD(message.payload))
        }
        Some(publish)
      case None => None
    }
  }


  def socketClose = {
  }

  def clear = {
    topics = List()
    readyQueue.foreach(chunk => chunk.clear)
    readyQueue = List()
    workQueue = List()
  }

  private def nextPacketId = {
    packetIdGenerator = {
      if (packetIdGenerator < 0)
        0
      else (packetIdGenerator + 1).toShort
    }
    packetIdGenerator
  }

}

object Storage {
  def apply(sessionName: String) = new Storage(sessionName)
}