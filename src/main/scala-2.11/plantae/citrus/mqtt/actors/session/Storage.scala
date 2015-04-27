package plantae.citrus.mqtt.actors.session

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
    count = count + 1
    readyQueue = readyQueue :+ ReadyMessage(payload, qos, retain, topic)
  }

  def complete(packetId: Short) =
    workQueue = workQueue.filterNot(_.packetId match {
      case Some(x) => x.value == packetId
      case None => false
    })

  def nextMessage: Option[PUBLISH] = {

    if (workQueue.size > 0) {
      println("readyQueue size " + readyQueue.size + "  workQueue : " + workQueue.size);
      None
    } else readyQueue match {
      case head :: tail =>
        readyQueue = tail
        val publish = PUBLISH(false, INT(head.qos), head.retain, STRING(head.topic), Some(INT(nextPacketId)), PUBLISHPAYLOAD(head.payload))
        workQueue = workQueue :+ publish
        Some(publish)
      case List() => println("readyQueue size " + readyQueue.size + "  workQueue : " + workQueue.size); None
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
