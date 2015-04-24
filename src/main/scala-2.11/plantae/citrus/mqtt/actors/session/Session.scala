package plantae.citrus.mqtt.actors.session

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.ask
import plantae.citrus.mqtt.actors._
import plantae.citrus.mqtt.actors.directory._
import plantae.citrus.mqtt.actors.topic.{Subscribe, TopicOutMessage, TopicResponse}
import plantae.citrus.mqtt.dto._
import plantae.citrus.mqtt.dto.connect.{CONNACK, CONNECT, DISCONNECT, ReturnCode}
import plantae.citrus.mqtt.dto.ping.{PINGREQ, PINGRESP}
import plantae.citrus.mqtt.dto.publish._
import plantae.citrus.mqtt.dto.subscribe.{SUBACK, SUBSCRIBE, TopicFilter}
import plantae.citrus.mqtt.dto.unsubscribe.{UNSUBACK, UNSUBSCRIBE}

import scala.concurrent.Await
import scala.concurrent.duration.Duration


case class MQTTInboundPacket(mqttPacket: Packet)

case class MQTTOutboundPacket(mqttPacket: Packet)

sealed trait SessionRequest

sealed trait SessionResponse

case object SessionReset extends SessionRequest

case object SessionResetAck extends SessionResponse

case object SessionKeepAliveTimeOut extends SessionRequest

case object ClientCloseConnection extends SessionRequest

class SessionCreator extends Actor with ActorLogging {
  override def receive = {
    case clientId: String => {
      log.debug("new session is created [{}]", clientId)
      sender ! context.actorOf(Props[Session], clientId)
    }
  }
}

class Session extends DirectoryMonitorActor with ActorLogging {
  implicit val timeout = akka.util.Timeout(5, TimeUnit.SECONDS)

  var connectionStatus: Option[ConnectionStatus] = None
  val storage = new Storage()

  override def actorType: ActorType = TypeSession

  override def receive: Receive = {
    case MQTTInboundPacket(packet) => handleMQTTPacket(packet, sender)
    case sessionRequest: SessionRequest => handleSession(sessionRequest)
    case topicResponse: TopicResponse => handleTopicPacket(topicResponse, sender)
    case OutboundPublishComplete => invokePublish
    case RegisterAck(name) =>
    case everythingElse => log.error("unexpected message : {}", everythingElse)
  }

  def handleTopicPacket(response: TopicResponse, sender: ActorRef) {
    response match {
      case message: TopicOutMessage =>
        storage.persist(message.payload, message.qos, message.retain, message.topic)
        invokePublish
      case anyOtherTopicMessage => log.error(" unexpected topic message {}", anyOtherTopicMessage)
    }
  }


  def handleSession(command: SessionRequest): Unit = command match {

    case SessionReset => {
      log.debug("session reset : " + self.path.name)
      connectionStatus = None
      storage.clear
      sender ! SessionResetAck
    }

    case SessionKeepAliveTimeOut => {
      log.debug("No keep alive request!!!!")
    }

    case ClientCloseConnection => {
      log.debug("ClientCloseConnection : " + self.path.name)
      val currentConnectionStatus = connectionStatus
      connectionStatus = None
      currentConnectionStatus match {
        case Some(x) =>
          x.handleWill
          x.destory
        case None =>
      }
    }

  }

  def handleMQTTPacket(packet: Packet, bridge: ActorRef): Unit = {
    connectionStatus match {
      case Some(x) => x.resetTimer
      case None =>
    }

    packet match {
      case mqtt: CONNECT =>
        connectionStatus = Some(ConnectionStatus(mqtt.will, mqtt.keepAlive.value, self, sender))
        bridge ! MQTTOutboundPacket(CONNACK(true, ReturnCode.connectionAccepted))
        invokePublish
      case PINGREQ =>
        bridge ! MQTTOutboundPacket(PINGRESP)
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
          case None => log.error("can't find publish outbound actor {}", PublishConstant.outboundPrefix + mqtt.packetId.value)

        }
      case mqtt: PUBCOMB =>
        context.child(PublishConstant.outboundPrefix + mqtt.packetId.value) match {
          case Some(x) => x ! mqtt
          case None => log.error("can't find publish outbound actor {}", PublishConstant.outboundPrefix + mqtt.packetId.value)
        }

      case DISCONNECT => {
        val currentConnectionStatus = connectionStatus
        connectionStatus = None
        currentConnectionStatus match {
          case Some(x) =>
            x.destory
          case None =>
        }

      }

      case subscribe: SUBSCRIBE =>
        val subscribeResult = subscribeTopics(subscribe.topicFilter)
        sender ! MQTTOutboundPacket(SUBACK(subscribe.packetId, subscribeResult))

      case unsubscribe: UNSUBSCRIBE =>
        unsubscribeTopics(unsubscribe.topicFilter)
        sender ! MQTTOutboundPacket(UNSUBACK(unsubscribe.packetId))
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

  def invokePublish = {
    val session = self
    connectionStatus match {
      case Some(tcp) =>
        storage.nextMessage match {
          case Some(x) =>

            val actorName = PublishConstant.outboundPrefix + (x.packetId match {
              case Some(y) => y.value
              case None => UUID.randomUUID().toString
            })
            context.child(actorName) match {
              case Some(actor) => actor ! x
              case None => context.actorOf(Props(classOf[OutboundPublisher], tcp.socket, session), actorName) ! x
            }
          case None => log.debug("no message")
        }
      case None => log.debug("no connection")
    }
  }

}