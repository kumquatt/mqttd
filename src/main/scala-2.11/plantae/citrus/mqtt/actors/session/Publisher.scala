package plantae.citrus.mqtt.actors.session

import akka.actor._
import plantae.citrus.mqtt.actors.SystemRoot
import plantae.citrus.mqtt.actors.directory.{DirectoryTopicRequest, DirectoryTopicResult}
import plantae.citrus.mqtt.actors.topic.{TopicInMessage, TopicInMessageAck}
import plantae.citrus.mqtt.packet._


case object Uninitialized

sealed trait State

sealed trait Inbound extends State

sealed trait Outbound extends State

case class OutboundPublishDone(packetId: Option[Int]) extends Outbound

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

  startWith(WaitPublish, null)

  when(WaitPublish) {
    case Event(publish: PublishPacket, _) =>
      log.info(" actor-name : {} , status : {}", self.path.name, "WaitPublish")
      client ! MQTTOutboundPacket(publish)
      publish.fixedHeader.qos match {
        case 0 =>
          session ! OutboundPublishDone(None)
          stop(FSM.Shutdown)
        case 1 => goto(WaitPubAck)
        case 2 => goto(WaitPubRec)
        case anyOtherCase =>
          log.debug("unexpected case : {} , actor-name : {} , status : {}", anyOtherCase, self.path.name, "WaitPublish")
          stop(FSM.Shutdown)
      }

    case anyOtherCase =>
      log.error("unexpected case : {} , actor-name : {} , status : {}", anyOtherCase, self.path.name, "WaitPublish")
      stop(FSM.Shutdown)

  }

  when(WaitPubAck) {
    case Event(PubAckPacket(fixedHeader, packetId), _) =>

      log.debug("expected : {} , real {}, actor-name : {} , status : {}", publishActor.path.name.drop(PublishConstant.outboundPrefix.length).toShort, packetId, self.path.name, "WaitPubAck")
      session ! OutboundPublishDone(Some(packetId))
      stop(FSM.Shutdown)

    case anyOtherCase =>
      log.debug("unexpected case : {} , actor-name : {} , status : {}", anyOtherCase, self.path.name, "WaitPubAck")
      stop(FSM.Shutdown)
  }

  when(WaitPubRec) {

    case Event(PubRecPacket(fixedHeader, packetId), _) =>
      log.debug("expected : {} , real {}, actor-name : {} , status : {}", publishActor.path.name.drop(PublishConstant.outboundPrefix.length).toShort, packetId, self.path.name, "WaitPubRec")

      client ! MQTTOutboundPacket(PubRelPacket(packetId = packetId))

      goto(WaitPubComb)

    case anyOtherCase =>
      log.debug("unexpected case : {} , actor-name : {} , status : {}", anyOtherCase, self.path.name, "WaitPubRec")
      stop(FSM.Shutdown)

  }

  when(WaitPubComb) {

    case Event(PubCompPacket(fixedHeader, packetId), _) =>
      log.debug("expected : {} , real {}, actor-name : {} , status : {}", publishActor.path.name.drop(PublishConstant.outboundPrefix.length).toShort, packetId, self.path.name, "WaitPubComb")

      session ! OutboundPublishDone(Some(packetId))
      stop(FSM.Shutdown)

    case anyOtherCase =>
      log.error("unexpected case : {} , actor-name : {} , status : {}", anyOtherCase, self.path.name, "WaitPubComb")
      stop(FSM.Shutdown)

  }

  whenUnhandled {
    case e: Event =>
      stop(FSM.Shutdown)
  }

  initialize()

}


class InboundPublisher(client: ActorRef, qos: Short) extends FSM[Inbound, Any] with ActorLogging {
  val publishActor = self

  val packetId: Option[Int] = qos match {
    case 0 => None
    case anyOther if (anyOther > 0) => Some(
      publishActor.path.name.drop(PublishConstant.inboundPrefix.length).toInt
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

  startWith(WaitPublish, null)

  when(WaitPublish) {
    case Event(publish: PublishPacket, _) =>
      log.debug(" actor-name : {} , status : {}", self.path.name, "WaitPublish")
      SystemRoot.directoryProxy.tell(DirectoryTopicRequest(publish.topic), context.actorOf(Props(new Actor {
        def receive = {
          case DirectoryTopicResult(name, actors) =>
            actors.foreach(actor =>
              actor.tell(
                TopicInMessage(publish.payload.toArray, publish.fixedHeader.qos, publish.fixedHeader.retain,
                  publish.packetId), publishActor)
            )
          //            context.stop(self)
        }
      }
      )))
      publish.fixedHeader.qos match {
        case 0 => goto(WaitTopicResponseQos0)
        case 1 => goto(WaitTopicResponseQos1)
        case 2 => goto(WaitTopicResponseQos2)
        case other => stop(FSM.Shutdown)
      }
    case anyOtherCase =>
      log.error("unexpected case : {} , actor-name : {} , status : {}", anyOtherCase, self.path.name, "WaitPublish")
      stop(FSM.Shutdown)

  }



  when(WaitTopicResponseQos0) {
    case Event(TopicInMessageAck, _) =>
      log.debug(" actor-name : {} , status : {}", self.path.name, "WaitTopicResponseQos0")

      stop(FSM.Shutdown)

    case anyOtherCase =>
      log.error("unexpected case : {} , actor-name : {} , status : {}", anyOtherCase, self.path.name, "WaitTopicResponseQos0")
      stop(FSM.Shutdown)


  }


  when(WaitTopicResponseQos1) {
    case Event(TopicInMessageAck, _) =>
      packetId match {
        case Some(x) =>
          log.debug(" actor-name : {} , status : {}", self.path.name, "WaitTopicResponseQos1")

          client ! MQTTOutboundPacket(PubAckPacket(packetId = x))
          stop(FSM.Shutdown)
        case None => stop(FSM.Shutdown)
      }
    case anyOtherCase =>
      log.error("unexpected case : {} , actor-name : {} , status : {}", anyOtherCase, self.path.name, "WaitTopicResponseQos1")
      stop(FSM.Shutdown)

  }

  when(WaitTopicResponseQos2) {
    case Event(TopicInMessageAck, _) =>
      packetId match {
        case Some(x) =>
          log.debug(" actor-name : {} , status : {}", self.path.name, "WaitTopicResponseQos2")

          client ! MQTTOutboundPacket(PubRecPacket(packetId = x))
          goto(WaitPubRel)
        case None => stop(FSM.Shutdown)
      }
    case anyOtherCase =>
      log.error("unexpected case : {} , actor-name : {} , status : {}", anyOtherCase, self.path.name, "WaitTopicResponseQos2")
      stop(FSM.Shutdown)

  }

  when(WaitPubRel) {
    case Event(PubRelPacket(fixedHeader, (pubRelPacketId)), _) =>
      packetId match {
        case Some(x) if (x == pubRelPacketId) =>
          log.debug(" actor-name : {} , status : {}", self.path.name, "WaitPubRel")

          client ! MQTTOutboundPacket(PubCompPacket(packetId = x))
          stop(FSM.Shutdown)
        case Some(x) =>
          log.debug("expected is {} , but real is {} actor-name : {} , status : {}", x, pubRelPacketId, self.path.name, "WaitPubRel")
          stop(FSM.Shutdown)
        case None =>
          log.error("expected is {} , but real is {} actor-name : {} , status : {}", None, pubRelPacketId, self.path.name, "WaitPubRel")
          stop(FSM.Shutdown)

      }

    case anyOtherCase =>
      log.error("unexpected case : {} , actor-name : {} , status : {}", anyOtherCase, self.path.name, "WaitPubRel")
      stop(FSM.Shutdown)
  }

  whenUnhandled {
    case e: Event =>
      stop(FSM.Shutdown)
  }

  initialize()
}