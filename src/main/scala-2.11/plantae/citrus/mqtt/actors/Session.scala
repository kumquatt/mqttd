package plantae.citrus.mqtt.actors


import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.event.Logging
import akka.pattern.ask
import plantae.citrus.mqtt.dto._
import plantae.citrus.mqtt.dto.connect.{CONNACK, CONNECT, DISCONNECT, ReturnCode, Will}
import plantae.citrus.mqtt.dto.ping.{PINGREQ, PINGRESP}
import plantae.citrus.mqtt.dto.publish._
import plantae.citrus.mqtt.dto.subscribe.{SUBACK, SUBSCRIBE, TopicFilter}
import plantae.citrus.mqtt.dto.unsubscribe.UNSUBSCRIBE

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext}


case class MqttInboundPacket(mqttPacket: Packet)

case class MqttOutboundPacket(mqttPacket: Packet)

sealed trait SessionCommand

case object SessionReset extends SessionCommand

case object SessionResetAck extends SessionCommand

case object SessionKeepAliveTimeOut extends SessionCommand

case object ClientCloseConnection extends SessionCommand

class SessionCreator extends Actor {
  private val logger = Logging(context.system, this)

  override def receive = {
    case clientId: String => {
      logger.info("new session is created [{}]", clientId)
      sender ! context.actorOf(Props[Session], clientId)
    }
  }
}

class Session extends DirectoryMonitorActor with ActorLogging {
  implicit val timeout = akka.util.Timeout(5, TimeUnit.SECONDS)
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  case class ConnectionStatus(will: Option[Will], keepAliveTime: Int)

  var connectionStatus: Option[ConnectionStatus] = None
  var keepAliveTimer: Option[Cancellable] = None


  override def actorType: ActorType = TypeSession

  override def receive: Receive = {
    case MqttInboundPacket(packet) => mqttPacket(packet, sender)
    case command: SessionCommand => session(command)
    case RegisterAck(name) => {
      log.info("receive register ack")
    }
    case everythingElse => println(everythingElse)
  }


  def session(command: SessionCommand): Unit = command match {

    case SessionReset => {
      log.info("session reset : " + self.toString())
      connectionStatus = None
      sender ! SessionResetAck
    }

    case SessionKeepAliveTimeOut => {
      log.info("No keep alive request!!!!")
    }

    case ClientCloseConnection => {
      //TODO : will process handling
      log.info("ClientCloseConnection : " + self.toString())
      cancelTimer
      connectionStatus = None
    }

  }

  def cancelTimer = {
    keepAliveTimer match {
      case Some(x) => x.cancel()
      case None =>
    }
  }

  def resetTimer = {
    cancelTimer
    connectionStatus match {
      case Some(x) =>
        keepAliveTimer = Some(ActorContainer.system.scheduler.scheduleOnce(
          FiniteDuration(x.keepAliveTime, TimeUnit.SECONDS), self, SessionKeepAliveTimeOut)
        )
      case None =>
    }
  }

  def mqttPacket(packet: Packet, bridge: ActorRef): Unit = {

    packet match {
      case mqtt: CONNECT =>
        connectionStatus = Some(ConnectionStatus(mqtt.will, mqtt.keepAlive.value))
        bridge ! MqttOutboundPacket(CONNACK(true, ReturnCode.connectionAccepted))
        resetTimer
      case PINGREQ =>
        resetTimer
        bridge ! MqttOutboundPacket(PINGRESP)
      case mqtt: PUBLISH => {
        context.actorOf(Props(classOf[InboundPublisher], sender, mqtt.qos.value), {
          mqtt.qos.value match {
            case 0 => UUID.randomUUID().toString
            case 1 => PublishConstant.inboundPrefix + mqtt.packetId.get.value
            case 2 => PublishConstant.inboundPrefix + mqtt.packetId.get.value
          }
        }) ! mqtt
      }
      case mqtt: PUBREL =>
        context.child(PublishConstant.inboundPrefix + mqtt.packetId.value) match {
          case Some(x) => x ! mqtt
          case None => log.error("can't find publish inbound actor {}",PublishConstant.inboundPrefix + mqtt.packetId.value)
        }
      case mqtt: PUBREC =>
        context.child(PublishConstant.outboundPrefix + mqtt.packetId.value) match {
          case Some(x) => x ! mqtt
          case None => log.error("can't find publish outbound actor {}",PublishConstant.inboundPrefix + mqtt.packetId.value)

        }
      case mqtt: PUBACK =>
        context.child(PublishConstant.outboundPrefix + mqtt.packetId.value) match {
          case Some(x) => x ! mqtt
          case None => log.error("can't find publish outbound actor {}",PublishConstant.inboundPrefix + mqtt.packetId.value)

        }
      case mqtt: PUBCOMB =>
        context.child(PublishConstant.outboundPrefix + mqtt.packetId.value) match {
          case Some(x) => x ! mqtt
          case None => log.error("can't find publish outbound actor {}",PublishConstant.inboundPrefix + mqtt.packetId.value)
        }

      case DISCONNECT => {
        connectionStatus = None
        cancelTimer
      }

      case subscribe: SUBSCRIBE =>
        val subscribeResult = subscribeToTopics(subscribe.topicFilter)

        subscribeResult.foreach(b =>
          log.info("... " + b)
        )
        sender ! MqttOutboundPacket(SUBACK(subscribe.packetId, subscribeResult))
      //        sender ! MqttOutboundPacket(SUBACK(subscribe.packetId ,subscribe.topicFilter.foldRight(List[BYTE]())((x,accum) => BYTE(0x00) ::accum )))


      case unsubscribe: UNSUBSCRIBE =>
        unsubscribeToTopics(unsubscribe.topicFilter)
    }

  }

  def subscribeToTopics(topicFilters: List[TopicFilter]): List[BYTE] = {
    val clientId = self.path.name

    val result = topicFilters.map(tp =>
      Await.result(ActorContainer.directory ? DirectoryReq(tp.topic.value, TypeTopic), Duration.Inf) match {
        case DirectoryResp(topicName, option) =>
          option ! Subscribe(self.path.name)
          BYTE(0x00)
      }
    )
    result
  }

  def unsubscribeToTopics(topics: List[STRING]) = {
  }

  def doConnect(connect: CONNECT): CONNACK = {
    CONNACK(true, ReturnCode.connectionAccepted)
  }

}


//sealed trait data
//
//case object uninitialized extends data
//
//case class fromClient(data: Packet) extends data
//
//case class toClient(data: Packet) extends data
//
//sealed trait state
//
//case object waitPublish extends state
//
//case object waitTopicResponseQos1 extends state
//
//case object waitTopicResponseQos2 extends state
//
//case object waitPubRel extends state
//
//case object complete extends state
//
//class PublishProcessor(client: ActorRef) extends FSM[state, data] with ActorLogging {
//  val processActor = self
//  startWith(waitPublish, uninitialized)
//
//  when(waitPublish) {
//    case Event(publish: PUBLISH, waitPublish) =>
//      val relay = context.actorOf(Props(new Actor {
//        override def receive = {
//          case DirectoryResp(name, actor) =>
//            actor.tell(TopicMessage(publish.data.value, publish.qos.value, publish.retain, publish.packetId match {
//              case Some(x) => Some(x.value)
//              case None => None
//            }
//            ), processActor)
//        }
//      }))
//      ActorContainer.directory.tell(DirectoryReq(publish.topic.value, TypeTopic), relay)
//
//      publish.qos.value match {
//        case 0 =>
//          goto(complete)
//        case 1 =>
//          goto(waitTopicResponseQos1)
//        case 2 =>
//          goto(waitTopicResponseQos2)
//      }
//  }
//
//  when(waitTopicResponseQos1) {
//    case Event(TopicMessageAck(packetId), waitPublish) =>
//      client ! MqttOutboundPacket(PUBACK(INT(packetId.toShort)))
//      goto(complete)
//  }
//
//  when(waitTopicResponseQos2) {
//    case Event(TopicMessageAck(packetId), waitPublish) =>
//      client ! MqttOutboundPacket(PUBREC(INT(packetId.toShort)))
//      goto(waitPubRel)
//  }
//
//  when(waitPubRel) {
//    case Event(PUBREL(packetId), waitPublish) =>
//      client ! MqttOutboundPacket(PUBCOMB((packetId)))
//      goto(complete) using toClient(PUBCOMB(packetId))
//
//  }
//
//  when(complete) {
//    case anyCase => stop()
//  }
//
//
//  whenUnhandled {
//    case e: Event =>
//      goto(complete)
//  }
//
//  initialize()
//}