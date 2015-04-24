package plantae.citrus.mqtt.actors.session

import akka.actor._
import plantae.citrus.mqtt.actors.ActorContainer
import plantae.citrus.mqtt.actors.directory.{DirectoryReq, DirectoryResp, TypeTopic}
import plantae.citrus.mqtt.actors.topic.{TopicInMessage, TopicInMessageAck}
import plantae.citrus.mqtt.dto.INT
import plantae.citrus.mqtt.dto.publish._


case object uninitialized

sealed trait state

sealed trait inbound extends state

sealed trait outbound extends state

case object OutboundPublishComplete extends outbound

case object waitPublish extends inbound with outbound

case object resume extends inbound with outbound

case object waitTopicResponseQos0 extends inbound

case object waitTopicResponseQos1 extends inbound

case object waitTopicResponseQos2 extends inbound

case object waitPubRel extends inbound

case object waitPubAck extends outbound

case object waitPubRec extends outbound

case object waitPubComb extends outbound

object PublishConstant {
  val inboundPrefix = "publish:inbound-"
  val outboundPrefix = "publish:outbound-"
}

class OutboundPublisher(client: ActorRef, session: ActorRef) extends FSM[outbound, Any] with ActorLogging {
  val publishActor = self

  override def preStart {
    log.debug("start publish outbound handler - " + self.path.name)
    super.preStart
  }

  override def postStop {
    log.debug("stop publish outbound handler - " + self.path.name)
    super.postStop
  }

  startWith(waitPublish, uninitialized)

  when(waitPublish) {
    case Event(publish: PUBLISH, waitPublish) =>
      client ! MqttOutboundPacket(publish)
      publish.qos.value match {
        case 0 =>
          session ! OutboundPublishComplete
          stop(FSM.Shutdown)
        case 1 => goto(waitPubAck)
        case 2 => goto(waitPubRec)
      }


  }

  when(waitPubAck) {
    case Event(PUBACK(packetId), waitPublish) =>
      session ! OutboundPublishComplete
      stop(FSM.Shutdown)
  }

  when(waitPubRec) {
    case Event(PUBREC(packetId), waitPublish) =>
      client ! MqttOutboundPacket(PUBREL(packetId))
      goto(waitPubComb)
  }

  when(waitPubComb) {
    case Event(PUBCOMB(packetId), waitPubRec) =>
      session ! OutboundPublishComplete
      stop(FSM.Shutdown)
  }

}


class InboundPublisher(client: ActorRef, qos: Short) extends FSM[inbound, Any] with ActorLogging {
  val publishActor = self

  val packetId: Option[Short] = qos match {
    case 0 => None
    case anyOther if (anyOther > 0) => Some(
      publishActor.path.name.drop(PublishConstant.inboundPrefix.length).toShort
    )
  }

  override def preStart {
    log.debug("start publish inbound handler - " + self.path.name)
    super.preStart

  }

  override def postStop {
    log.debug("stop publish inbound handler - " + self.path.name)
    super.postStop

  }


  startWith(waitPublish, uninitialized)

  when(waitPublish) {
    case Event(publish: PUBLISH, waitPublish) =>
      ActorContainer.invokeCallback(DirectoryReq(publish.topic.value, TypeTopic), context, {
        case DirectoryResp(name, actor) =>
          actor.tell(
            TopicInMessage(publish.data.value, publish.qos.value, publish.retain,
              publish.packetId match {
                case Some(x) => Some(x.value)
                case None => None
              }
            ), publishActor)
      }


      )

      publish.qos.value match {
        case 0 => goto(waitTopicResponseQos0)
        case 1 => goto(waitTopicResponseQos1)
        case 2 => goto(waitTopicResponseQos2)
        case other => stop(FSM.Shutdown)
      }
  }

  when(waitTopicResponseQos0) {
    case Event(TopicInMessageAck, waitPublish) =>
      stop(FSM.Shutdown)
  }


  when(waitTopicResponseQos1) {
    case Event(TopicInMessageAck, waitPublish) =>
      packetId match {
        case Some(x) =>
          client ! MqttOutboundPacket(PUBACK(INT(x)))
          stop(FSM.Shutdown)
        case None => stop(FSM.Shutdown)

      }
  }

  when(waitTopicResponseQos2) {
    case Event(TopicInMessageAck, waitPublish) =>
      packetId match {
        case Some(x) =>
          client ! MqttOutboundPacket(PUBREC(INT(x)))
          goto(waitPubRel)
        case None => stop(FSM.Shutdown)
      }

  }

  when(waitPubRel) {
    case Event(PUBREL(packetId), waitPublish) =>
      client ! MqttOutboundPacket(PUBCOMB((packetId)))
      stop(FSM.Shutdown)
  }

  whenUnhandled {
    case e: Event =>
      stop(FSM.Shutdown)
  }

  initialize()
}