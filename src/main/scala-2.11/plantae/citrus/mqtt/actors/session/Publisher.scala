package plantae.citrus.mqtt.actors.session

import akka.actor._
import plantae.citrus.mqtt.actors.ActorContainer
import plantae.citrus.mqtt.actors.directory.{DirectoryResp, DirectoryReq, TypeTopic}
import plantae.citrus.mqtt.actors.topic.{TopicMessageAck, TopicMessage}
import plantae.citrus.mqtt.dto.INT
import plantae.citrus.mqtt.dto.publish._


case object uninitialized

sealed trait state

sealed trait inbound extends state

sealed trait outbound extends state

case object waitPublish extends inbound with outbound

case object waitTopicResponseQos0 extends inbound

case object waitTopicResponseQos1 extends inbound

case object waitTopicResponseQos2 extends inbound

case object waitPubRel extends inbound

case object waitPubAck extends outbound

case object waitPubRec extends outbound

case object waitPubComb extends outbound

object PublishConstant {
  val inboundPrefix = "publish:inbound-"
  val outboundPrefix = "publish:inbound-"
}

class OutboundPublisher(client: ActorRef) extends FSM[outbound, Any] with ActorLogging {
  val publishActor = self

  override def preStart {
    log.info("start publish outbound handler - " + self.path.name)
    super.preStart
  }

  override def postStop {
    log.info("stop publish outbound handler - " + self.path.name)
    super.postStop

  }

  startWith(waitPublish, uninitialized)

  when(waitPublish) {
    case Event(publish: PUBLISH, waitPublish) =>
      client ! MqttOutboundPacket(publish)
      publish.qos.value match {
        case 0 => stop(FSM.Shutdown)
        case 1 => goto(waitPubAck)
        case 2 => goto(waitPubRec)
      }

  }

  when(waitPubAck) {
    case Event(PUBACK(packetId), waitPublish) => stop(FSM.Shutdown)
  }

  when(waitPubRec) {
    case Event(PUBREC(packetId), waitPublish) => goto(waitPubComb)
  }

  when(waitPubComb) {
    case Event(PUBCOMB(packetId), waitPubRec) => goto(waitPubComb)
  }

}


class InboundPublisher(client: ActorRef, qos: Short) extends FSM[inbound, Any] with ActorLogging {
  val publishActor = self

  val packetId: Option[Short] = qos match {
    case 0 => None
    case anyOther if (anyOther > 0) => Some(publishActor.path.name.drop(PublishConstant.inboundPrefix.length).toShort)
  }

  override def preStart {
    log.info("start publish inbound handler - " + self.path.name)
    super.preStart

  }

  override def postStop {
    log.info("stop publish inbound handler - " + self.path.name)
    super.postStop

  }


  startWith(waitPublish, uninitialized)

  when(waitPublish) {
    case Event(publish: PUBLISH, waitPublish) =>
      val relay = context.actorOf(Props(new Actor {
        override def receive = {
          case DirectoryResp(name, actor) =>
            actor.tell(
              TopicMessage(publish.data.value, publish.qos.value, publish.retain,
                publish.packetId match {
                  case Some(x) => Some(x.value)
                  case None => None
                }
              ), publishActor)
        }
      }))
      ActorContainer.directory.tell(DirectoryReq(publish.topic.value, TypeTopic), relay)

      publish.qos.value match {
        case 0 => goto(waitTopicResponseQos0)
        case 1 => goto(waitTopicResponseQos1)
        case 2 => goto(waitTopicResponseQos2)
        case other => stop(FSM.Shutdown)
      }
  }

  when(waitTopicResponseQos0) {
    case Event(TopicMessageAck, waitPublish) =>
      stop(FSM.Shutdown)
  }


  when(waitTopicResponseQos1) {
    case Event(TopicMessageAck, waitPublish) =>
      packetId match {
        case Some(x) =>
          client ! MqttOutboundPacket(PUBACK(INT(x)))
          stop(FSM.Shutdown)
        case None => stop(FSM.Shutdown)
      }
  }

  when(waitTopicResponseQos2) {
    case Event(TopicMessageAck, waitPublish) =>
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