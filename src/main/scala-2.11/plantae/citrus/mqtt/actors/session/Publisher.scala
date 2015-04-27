package plantae.citrus.mqtt.actors.session

import akka.actor._
import plantae.citrus.mqtt.actors.ActorContainer
import plantae.citrus.mqtt.actors.directory.{DirectoryReq, DirectoryResp2, TypeTopic}
import plantae.citrus.mqtt.actors.topic.{TopicInMessage, TopicInMessageAck}
import plantae.citrus.mqtt.dto.INT
import plantae.citrus.mqtt.dto.publish._


case object Uninitialized

sealed trait State

sealed trait Inbound extends State

sealed trait Outbound extends State

case class OutboundPublishDone(packetId: Short, qos: Short) extends Outbound

case class OutboundPubCompDone(packetId: Short) extends Outbound

case object WaitPublish extends Inbound with Outbound

case object Resume extends Inbound with Outbound

case object WaitTopicResponseQos0 extends Inbound

case object WaitTopicResponseQos1 extends Inbound

case object WaitTopicResponseQos2 extends Inbound

case object WaitPubRel extends Inbound

case object WaitPubAck extends Outbound

case object WaitPubRec extends Outbound

case object WaitPubComb extends Outbound

object PublishConstant {
  val inboundPrefix = "publish:inbound-"
  val outboundPrefix = "publish:outbound-"
}

class OutboundPublisher(client: ActorRef, session: ActorRef) extends FSM[Outbound, Any] with ActorLogging {
  val publishActor = self

  override def preStart {
    log.debug("start publish outbound handler - " + self.path.name)
    super.preStart
  }

  override def postStop {
    log.debug("stop publish outbound handler - " + self.path.name)
    super.postStop
  }

  startWith(WaitPublish, Uninitialized)

  when(WaitPublish) {
    case Event(publish: PUBLISH, waitPublish) =>
      client ! MQTTOutboundPacket(publish)
      publish.qos.value match {
        case 0 =>
          session ! OutboundPublishDone(0, 0)
          stop(FSM.Shutdown)
        case 1 => goto(WaitPubAck)
        case 2 => goto(WaitPubRec)
      }
  }

  when(WaitPubAck) {
    case Event(PUBACK(packetId), waitPublish) =>
      session ! OutboundPublishDone(packetId.value, 1)
      stop(FSM.Shutdown)
  }

  when(WaitPubRec) {
    case Event(PUBREC(packetId), waitPublish) =>
      session ! OutboundPublishDone(packetId.value, 2)
      client ! MQTTOutboundPacket(PUBREL(packetId))
      goto(WaitPubComb)
  }

  when(WaitPubComb) {
    case Event(PUBCOMB(packetId), waitPubRec) =>
      session ! OutboundPubCompDone(packetId.value)
      stop(FSM.Shutdown)
  }
}


class InboundPublisher(client: ActorRef, qos: Short) extends FSM[Inbound, Any] with ActorLogging {
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


  startWith(WaitPublish, Uninitialized)

  when(WaitPublish) {
    case Event(publish: PUBLISH, waitPublish) =>
      ActorContainer.invokeCallback(DirectoryReq(publish.topic.value, TypeTopic), context, Props(new Actor {
        def receive = {
          case DirectoryResp2(name, actors) =>
            actors.foreach(actor =>
              actor.tell(
                TopicInMessage(publish.data.value, publish.qos.value, publish.retain,
                  publish.packetId match {
                    case Some(x) => Some(x.value)
                    case None => None
                  }
                ), publishActor)
            )
        }
      }
      )
      )

      publish.qos.value match {
        case 0 => goto(WaitTopicResponseQos0)
        case 1 => goto(WaitTopicResponseQos1)
        case 2 => goto(WaitTopicResponseQos2)
        case other => stop(FSM.Shutdown)
      }
  }

  when(WaitTopicResponseQos0) {
    case Event(TopicInMessageAck, waitPublish) =>
      stop(FSM.Shutdown)
  }


  when(WaitTopicResponseQos1) {
    case Event(TopicInMessageAck, waitPublish) =>
      packetId match {
        case Some(x) =>
          client ! MQTTOutboundPacket(PUBACK(INT(x)))
          stop(FSM.Shutdown)
        case None => stop(FSM.Shutdown)

      }
  }

  when(WaitTopicResponseQos2) {
    case Event(TopicInMessageAck, waitPublish) =>
      packetId match {
        case Some(x) =>
          client ! MQTTOutboundPacket(PUBREC(INT(x)))
          goto(WaitPubRel)
        case None => stop(FSM.Shutdown)
      }

  }

  when(WaitPubRel) {
    case Event(PUBREL(packetId), waitPublish) =>
      client ! MQTTOutboundPacket(PUBCOMB((packetId)))
      stop(FSM.Shutdown)
  }

  whenUnhandled {
    case e: Event =>
      stop(FSM.Shutdown)
  }

  initialize()
}