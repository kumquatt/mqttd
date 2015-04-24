package plantae.citrus.mqtt.actors.session

import java.util.concurrent.atomic.AtomicLong

import plantae.citrus.mqtt.dto.publish.PUBLISH
import plantae.citrus.mqtt.dto.{INT, PUBLISHPAYLOAD, STRING}

/**
 * Created by yinjae on 15. 4. 24..
 */
class Storage {

  case class message(payload: Array[Byte], qos: Short, retain: Boolean, topic: String)

  private val packetIdGenerator = new AtomicLong()
  private var topics: List[String] = List()
  private var messages: List[message] = List()
  private var messageInProcessing: List[PUBLISH] = List()

  def persist(payload: Array[Byte], qos: Short, retain: Boolean, topic: String) =
    messages = messages ++ List(message(payload, qos, retain, topic))

  def nextPacketId = packetIdGenerator.incrementAndGet().toShort

  def nextMessage: Option[PUBLISH] = {
    messages match {
      case head :: tail => messages = tail
        val publish = PUBLISH(false, INT(head.qos), head.retain, STRING(head.topic), Some(INT(nextPacketId)), PUBLISHPAYLOAD(head.payload))
        messageInProcessing = messageInProcessing ++ List(publish)
        Some(publish)
      case List() => None
    }
  }

  def clear = {
    topics = List()
    messages = List()
    messageInProcessing = List()
  }
}
