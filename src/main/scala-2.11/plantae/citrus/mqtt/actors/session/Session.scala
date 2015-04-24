package plantae.citrus.mqtt.actors.session

import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import akka.actor._
import akka.event.Logging
import akka.pattern.ask
import plantae.citrus.mqtt.actors._
import plantae.citrus.mqtt.actors.directory._
import plantae.citrus.mqtt.actors.topic.{Subscribe, TopicOutMessage}
import plantae.citrus.mqtt.dto._
import plantae.citrus.mqtt.dto.connect.{CONNACK, CONNECT, DISCONNECT, ReturnCode, Will}
import plantae.citrus.mqtt.dto.ping.{PINGREQ, PINGRESP}
import plantae.citrus.mqtt.dto.publish._
import plantae.citrus.mqtt.dto.subscribe.{SUBACK, SUBSCRIBE, TopicFilter}
import plantae.citrus.mqtt.dto.unsubscribe.{UNSUBACK, UNSUBSCRIBE}

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
  val packetIdGen = new AtomicInteger()
  var socket:ActorRef = null

  override def actorType: ActorType = TypeSession

  override def receive: Receive = {
    case MqttInboundPacket(packet) => mqttPacket(packet, sender)
    case command: SessionCommand => session(command)
    case topicMessage: TopicOutMessage => topicPacket(topicMessage, sender)
    case RegisterAck(name) => {
      log.info("receive register ack")
    }
    case everythingElse => println(everythingElse)
  }

  def topicPacket(message: TopicOutMessage, sender: ActorRef) {
    val packetId = packetIdGen.incrementAndGet().toShort
    context.actorOf(Props(classOf[OutboundPublisher], socket, Some(packetId)), PublishConstant.outboundPrefix + packetId) !(message.payload, message.qos.toShort, message.retain, message.topic)
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

  def mqttPacket(packet: Packet, bridge: ActorRef): Unit = {
    resetTimer
    packet match {
      case mqtt: CONNECT =>
        socket = sender
        connectionStatus = Some(ConnectionStatus(mqtt.will, mqtt.keepAlive.value))
        bridge ! MqttOutboundPacket(CONNACK(true, ReturnCode.connectionAccepted))
      case PINGREQ =>
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
          case None => log.error("can't find publish inbound actor {}", PublishConstant.inboundPrefix + mqtt.packetId.value)
        }
      case mqtt: PUBREC =>
        context.child(PublishConstant.outboundPrefix + mqtt.packetId.value) match {
          case Some(x) => x ! mqtt
          case None => log.error("can't find publish outbound actor {}", PublishConstant.outboundPrefix + mqtt.packetId.value)

        }
      case mqtt: PUBACK =>
        context.child(PublishConstant.outboundPrefix + mqtt.packetId.value) match {
          case Some(x) => x ! mqtt
            println(x)
          case None => log.error("can't find publish outbound actor {}", PublishConstant.outboundPrefix + mqtt.packetId.value)

        }
      case mqtt: PUBCOMB =>
        context.child(PublishConstant.outboundPrefix + mqtt.packetId.value) match {
          case Some(x) => x ! mqtt
          case None => log.error("can't find publish outbound actor {}", PublishConstant.outboundPrefix + mqtt.packetId.value)
        }

      case DISCONNECT => {
        connectionStatus = None
        cancelTimer
      }

      case subscribe: SUBSCRIBE =>
        val subscribeResult = subscribeTopics(subscribe.topicFilter)
        sender ! MqttOutboundPacket(SUBACK(subscribe.packetId, subscribeResult))

      case unsubscribe: UNSUBSCRIBE =>
        unsubscribeTopics(unsubscribe.topicFilter)
        sender ! MqttOutboundPacket(UNSUBACK(unsubscribe.packetId))
    }

  }

  def subscribeTopics(topicFilters: List[TopicFilter]): List[BYTE] = {
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

  def unsubscribeTopics(topics: List[STRING]) = {
    topics.foreach(x => {
      ActorContainer.invokeCallback(DirectoryReq(x.value, TypeTopic), context, {
        case DirectoryResp(name, topicActor) =>
          topicActor != UNSUBSCRIBE
      })
    }
    )

  }

  def doConnect(connect: CONNECT): CONNACK = {
    CONNACK(true, ReturnCode.connectionAccepted)
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


}