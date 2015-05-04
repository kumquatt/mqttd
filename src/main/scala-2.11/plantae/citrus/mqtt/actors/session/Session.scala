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

case class MQTTInboundPacket(mqttPacket: Packet)

case class MQTTOutboundPacket(mqttPacket: Packet)

sealed trait SessionRequest

case class SessionCreateRequest(clientId: String)

case class SessionCreateResponse(clientId: String, session: ActorRef)

case class SessionExistRequest(clientId: String)

case class SessionExistResponse(clientId: String, session: Option[ActorRef])

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

    case SessionCreateRequest(clientId: String) => {
      context.child(clientId) match {
        case Some(x) => sender ! x
        case None => log.debug("new session is created [{}]", clientId)
          sender ! SessionCreateResponse(clientId, context.actorOf(Props[Session], clientId))
      }
    }

    case SessionExistRequest(clientId) =>
      sender ! SessionExistResponse(clientId, context.child(clientId))

  }
}

class Session extends Actor with ActorLogging {
  implicit val timeout = akka.util.Timeout(5, TimeUnit.SECONDS)

  private var connectionStatus: Option[ConnectionStatus] = None
  private val storage = Storage(self.path.name)

  override def postStop = {
    log.info("shut down session {}", self)
    connectionStatus match {
      case Some(x) => x.cancelTimer
        connectionStatus = None
      case None =>
    }
    storage.clear
  }

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

    case SessionKeepAliveTimeOut => {
      connectionStatus match {
        case Some(x) => context.stop(x.socket)
        case None =>
      }
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
        connectionStatus match {
          case Some(x) =>
            x.destory
          case None =>
        }
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
        val actorName = inboundActorName(mqtt.packetId.value.toString)
        context.child(actorName) match {
          case Some(x) => x ! mqtt
          case None => log.error("[PUBREL] can't find publish inbound actor {}", actorName)
        }

      case mqtt: PUBREC =>
        val actorName = outboundActorName(mqtt.packetId.value.toString)
        context.child(actorName) match {
          case Some(x) => x ! mqtt
          case None => log.error("[PUBREC] can't find publish outbound actor {}", actorName)
        }

      case mqtt: PUBACK =>
        val actorName = outboundActorName(mqtt.packetId.value.toString)
        context.child(actorName) match {
          case Some(x) => x ! mqtt
          case None => log.error("[PUBACK] can't find publish outbound actor {} current child actors : {} packetId : {}", actorName,
            context.children.foldLeft(List[String]())((x, y) => {
              x :+ y.path.name
            }), mqtt.packetId)
        }

      case mqtt: PUBCOMB =>
        val actorName = outboundActorName(mqtt.packetId.value.toString)
        context.child(actorName) match {
          case Some(x) => x ! mqtt
          case None => log.error("[PUBCOMB] can't find publish outbound actor {} ", actorName)
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
    topicFilters.map(tp => {
      context.actorOf(Props(new Actor with ActorLogging {
        override def receive = {
          case request: DirectoryReq =>
            SystemRoot.directoryProxy ? request
          case DirectoryTopicResult(topicName, options) =>
            options.foreach(actor => actor ! Subscribe(self.path.name))
            context.stop(self)
        }
      })) ! DirectoryReq(tp.topic.value, TypeTopic)
      BYTE(0x00)
    }
    )
  }

  def unsubscribeTopics(topics: List[STRING]) = {
    topics.foreach(x => {
      SystemRoot.directoryProxy.tell(DirectoryReq(x.value, TypeTopic), context.actorOf(Props(new Actor {
        def receive = {
          case DirectoryTopicResult(name, topicActors) =>
            topicActors.foreach(actor => actor ! Unsubscribe(self.path.name))
          //          topicActor != UNSUBSCRIBE
        }
      })))

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