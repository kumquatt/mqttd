package plantae.citrus.mqtt.actors.session

import org.slf4j.{Logger, LoggerFactory}
import plantae.citrus.mqtt.dto.publish.PUBLISH
import plantae.citrus.mqtt.dto.{INT, PUBLISHPAYLOAD, STRING}

/**
 * Created by yinjae on 15. 4. 24..
 */
class Storage {

  var count = 0

  case class ReadyMessage(payload: Array[Byte], qos: Short, retain: Boolean, topic: String)

  private var packetIdGenerator: Short = 0
  private var topics: List[String] = List()
  private var readyQueue: List[ReadyMessage] = List()
  private var workQueue: List[PUBLISH] = List()

  def persist(payload: Array[Byte], qos: Short, retain: Boolean, topic: String) = {
    readyQueue = readyQueue :+ ReadyMessage(payload, qos, retain, topic)
  }


  def complete(packetId: Option[Short]) =
    packetId match {
      case Some(y) => workQueue = workQueue.filterNot(_.packetId match {
        case Some(x) => x.value == y
        case None => false
      })
      case None =>
    }

  def nextMessage: Option[PUBLISH] = {

    if (workQueue.size > 0) {
      None
    } else readyQueue match {
      case head :: tail =>
        readyQueue = tail
        val publish = head.qos match {
          case x if (x > 0) =>
            val publish = PUBLISH(false, INT(head.qos), head.retain, STRING(head.topic), Some(INT(nextPacketId)), PUBLISHPAYLOAD(head.payload))
            workQueue = workQueue :+ publish
            publish

          case x if (x == 0) =>
            PUBLISH(false, INT(head.qos), head.retain, STRING(head.topic), None, PUBLISHPAYLOAD(head.payload))
        }
        Some(publish)

      case List() => None
    }
  }


  def socketClose = {
  }

  def clear = {
    topics = List()
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
