package plantae.citrus.mqtt.actors.session

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.ask
import plantae.citrus.mqtt.actors._
import plantae.citrus.mqtt.actors.directory._
import plantae.citrus.mqtt.actors.topic.{Subscribe, TopicOutMessage, TopicResponse, Unsubscribe}
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

class SessionRoot extends Actor with ActorLogging {
  override def receive = {
    case clientId: String => {
      context.child(clientId) match {
        case Some(x) => sender ! x
        case None => log.debug("new session is created [{}]", clientId)
          sender ! context.actorOf(Props[Session], clientId)
      }
    }
  }
}

class Session extends Actor with ActorLogging {
  implicit val timeout = akka.util.Timeout(5, TimeUnit.SECONDS)

  var connectionStatus: Option[ConnectionStatus] = None
  val storage = Storage(self.path.name)


  override def receive: Receive = {
    case MQTTInboundPacket(packet) => handleMQTTPacket(packet, sender)
    case sessionRequest: SessionRequest => handleSession(sessionRequest)
    case topicResponse: TopicResponse => handleTopicPacket(topicResponse, sender)
    case x: Outbound => handleOutboundPublish(x)
    case everythingElse => log.error("unexpected message : {}", everythingElse)
  }

  def handleOutboundPublish(x: Outbound) = {
    x match {
      case publishDone: OutboundPublishDone =>
        storage.complete(publishDone.packetId)
        invokePublish
      case anyOtherCase => unhandled(anyOtherCase)
    }
  }

  def handleTopicPacket(response: TopicResponse, topic: ActorRef) {
    response match {
      case message: TopicOutMessage =>
        storage.persist(message.payload, message.qos, message.retain, message.topic)
        invokePublish
      case anyOtherTopicMessage =>
        log.error(" unexpected topic message {}", anyOtherTopicMessage)
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
          log.info(" disconnected without DISCONNECT : [{}]", self.path.name)
        case None => log.info(" disconnected after DISCONNECT : [{}]", self.path.name)
      }

    }
  }

  private def resetTimer = connectionStatus match {
    case Some(x) => x.resetTimer
    case None =>
  }

  private def inboundActorName(uniqueId: String) = PublishConstant.inboundPrefix + uniqueId

  private def outboundActorName(uniqueId: String) = PublishConstant.outboundPrefix + uniqueId

  def handleMQTTPacket(packet: Packet, bridge: ActorRef): Unit = {
    resetTimer
    packet match {
      case mqtt: CONNECT =>
        connectionStatus = Some(ConnectionStatus(mqtt.will, mqtt.keepAlive.value, self, context, sender))
        bridge ! MQTTOutboundPacket(CONNACK(true, ReturnCode.connectionAccepted))
        log.info("new connection establish : [{}]", self.path.name)
        invokePublish

      case PINGREQ =>
        bridge ! MQTTOutboundPacket(PINGRESP)

      case mqtt: PUBLISH => {
        context.actorOf(Props(classOf[InboundPublisher], sender, mqtt.qos.value), {
          mqtt.qos.value match {
            case 0 => inboundActorName(UUID.randomUUID().toString)
            case 1 => inboundActorName(mqtt.packetId.get.value.toString)
            case 2 => inboundActorName(mqtt.packetId.get.value.toString)
          }
        }) ! mqtt
      }

      case mqtt: PUBREL =>
        context.child(inboundActorName(mqtt.packetId.value.toString)) match {
          case Some(x) => x ! mqtt
          case None => log.error("can't find publish inbound actor {}", inboundActorName(mqtt.packetId.value.toString))
        }

      case mqtt: PUBREC =>
        context.child(outboundActorName(mqtt.packetId.value.toString)) match {
          case Some(x) => x ! mqtt
          case None => log.error("can't find publish outbound actor {}", outboundActorName(mqtt.packetId.value.toString))
        }

      case mqtt: PUBACK =>
        context.child(outboundActorName(mqtt.packetId.value.toString)) match {
          case Some(x) => x ! mqtt
          case None => log.error("can't find publish outbound actor {}", outboundActorName(mqtt.packetId.value.toString))
        }

      case mqtt: PUBCOMB =>
        context.child(outboundActorName(mqtt.packetId.value.toString)) match {
          case Some(x) => x ! mqtt
          case None => log.error("can't find publish outbound actor {}", outboundActorName(mqtt.packetId.value.toString))
        }

      case DISCONNECT => {
        val currentConnectionStatus = connectionStatus
        connectionStatus = None
        currentConnectionStatus match {
          case Some(x) => x.destory
          case None =>
        }
        log.info(" receive DISCONNECT : [{}]", self.path.name)

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
      Await.result(SystemRoot.directoryProxy ? DirectoryReq(tp.topic.value, TypeTopic), Duration.Inf) match {
        case DirectoryTopicResult(topicName, options) =>
          options.foreach(actor => actor ! Subscribe(self.path.name))
          //          option ! Subscribe(self.path.name)
          BYTE(0x00)
      }
    )
    result
  }

  def unsubscribeTopics(topics: List[STRING]) = {
    topics.foreach(x => {
      SystemRoot.invokeCallback(DirectoryReq(x.value, TypeTopic), context, Props(new Actor {
        def receive = {
          case DirectoryTopicResult(name, topicActors) =>
            topicActors.foreach(actor => actor ! Unsubscribe(self.path.name))
          //          topicActor != UNSUBSCRIBE
        }
      }))
    }
    )

  }

  def invokePublish = {
    val session = self
    connectionStatus match {
      case Some(client) =>
        storage.nextMessage match {
          case Some(x) =>
            val actorName = PublishConstant.outboundPrefix + (x.packetId match {
              case Some(y) => y.value
              case None => UUID.randomUUID().toString
            })

            context.child(actorName) match {
              case Some(actor) =>
                log.debug("using exist actor publish  complete {} ", actorName)
                actor ! x

              case None =>
                log.debug("create new actor publish  complete {} ", actorName)
                context.actorOf(Props(classOf[OutboundPublisher], client.socket, session), actorName) ! x

            }

          case None => log.debug("invoke publish but no message : child actor count - {} ", context.children.foldLeft(List[String]())((x, ac) => x :+ ac.path.name))
        }
      case None => log.debug("invoke publish but no connection : child actor count - {}", context.children.foldLeft(List[String]())((x, ac) => x :+ ac.path.name))
    }
  }

}