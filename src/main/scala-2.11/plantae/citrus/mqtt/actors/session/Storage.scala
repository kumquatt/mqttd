package plantae.citrus.mqtt.actors.session

import java.util.concurrent.atomic.AtomicLong

import plantae.citrus.mqtt.dto.publish.PUBLISH
import plantae.citrus.mqtt.dto.{INT, PUBLISHPAYLOAD, STRING}

/**
 * Created by yinjae on 15. 4. 24..
 */
class Storage {
  def progress(packetId: Short) = {
    val contains = messageInProcessing.exists(_.packetId match {
      case Some(x) => x.value == packetId
      case None => false
    })
    messageInProcessing = messageInProcessing.filterNot(_.packetId match {
      case Some(x) => x.value == packetId
      case None => false
    })
    waitingPubRel = waitingPubRel ++ List(packetId)
  }

  def completeQos2(packetId : Short) = {
    waitingPubRel = waitingPubRel.filterNot(_ == packetId)
  }
  def completeQos1(packetId: Short) = {
    messageInProcessing = messageInProcessing.filterNot(_.packetId match {
      case Some(x) => x.value == packetId
      case None => false
    })
  }


  case class message(payload: Array[Byte], qos: Short, retain: Boolean, topic: String)

  private val packetIdGenerator = new AtomicLong()
  private var topics: List[String] = List()
  private var messages: List[message] = List()
  private var messageInProcessing: List[PUBLISH] = List()
  private var waitingPubRel: List[Short] = List()
  private var messageInRedo: List[PUBLISH] = List()

  def persist(payload: Array[Byte], qos: Short, retain: Boolean, topic: String) =
    messages = messages ++ List(message(payload, qos, retain, topic))


  def nextMessage: Option[PUBLISH] = {
    messageInRedo match {
      case head :: tail => messageInRedo = tail
        messageInProcessing = messageInProcessing ++ List(head)
        Some(head)
      case List() => messages match {
        case head :: tail => messages = tail
          val publish = PUBLISH(false, INT(head.qos), head.retain, STRING(head.topic), Some(INT(nextPacketId)), PUBLISHPAYLOAD(head.payload))
          messageInProcessing = messageInProcessing ++ List(publish)
          Some(publish)
        case List() => None
      }
    }
  }


  def socketClose = {
    messageInRedo = messageInRedo ++ messageInProcessing
    messageInProcessing = List()
  }

  def clear = {
    topics = List()
    messages = List()
    messageInProcessing = List()
    messageInRedo = List()
  }

  private def nextPacketId = packetIdGenerator.incrementAndGet().toShort

}
