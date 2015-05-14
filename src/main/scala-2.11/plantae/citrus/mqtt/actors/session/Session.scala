package plantae.citrus.mqtt.actors.session

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor._
import plantae.citrus.mqtt.actors.SystemRoot
import plantae.citrus.mqtt.actors.topic.{Subscribe, Subscribed, TopicNameQos}
import plantae.citrus.mqtt.dto.connect.{ReturnCode, Will}
import plantae.citrus.mqtt.packet._
import scodec.bits.ByteVector

case class MQTTInboundPacket(mqttPacket: ControlPacket)

case class MQTTOutboundPacket(mqttPacket: ControlPacket)

sealed trait SessionRequest

case class SessionCreateRequest(clientId: String)

case class SessionCreateResponse(clientId: String, session: ActorRef)

case class SessionExistRequest(clientId: String)

case class SessionExistResponse(clientId: String, session: Option[ActorRef])

case object SessionKeepAliveTimeOut extends SessionRequest

case object ClientCloseConnection extends SessionRequest

case class PublishMessage(topicName: String, qos: Short, payload: ByteVector)

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
  private val storage = Storage(self)

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
    case publish: PublishMessage => handleTopicPacket(publish)
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

  def handleTopicPacket(response: PublishMessage) {

    storage.persist(response.payload.toArray, response.qos, false, response.topicName)
    invokePublish
  }


  def handleSession(command: SessionRequest): Unit = command match {

    case SessionKeepAliveTimeOut =>
      log.debug("Session TimeOut : " + self.path.name)

      connectionStatus match {
        case Some(x) =>
          x.destroyAbnormally
        case None =>
      }

      connectionStatus = None
      storage.socketClose

      context.children.foreach(child => {
        if (child.path.name.startsWith(PublishConstant.inboundPrefix) || child.path.name.startsWith(PublishConstant.outboundPrefix))
          context.stop(child)
      })


    case ClientCloseConnection => {
      log.debug("ClientCloseConnection : " + self.path.name)
      val currentConnectionStatus = connectionStatus
      connectionStatus = None
      storage.socketClose
      context.children.foreach(child => {
        if (child.path.name.startsWith(PublishConstant.inboundPrefix) || child.path.name.startsWith(PublishConstant.outboundPrefix))
          context.stop(child)
      })

      currentConnectionStatus match {
        case Some(x) =>
          x.destroyAbnormally
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

  def handleMQTTPacket(packet: ControlPacket, bridge: ActorRef): Unit = {

    resetTimer

    packet match {
      case mqtt: ConnectPacket =>
        connectionStatus match {
          case Some(x) =>
            x.destroyAbnormally
          case None =>
        }

        val mqttWill: Option[Will] = mqtt.variableHeader.willFlag match {
          case false => None
          case true => Some(Will(mqtt.variableHeader.willQoS, mqtt.variableHeader.willRetain, mqtt.willTopic.get, mqtt.willMessage.get))
        }

        connectionStatus = Some(ConnectionStatus(mqttWill, mqtt.variableHeader.keepAliveTime, self, context, sender))
        bridge ! MQTTOutboundPacket(ConnAckPacket(sessionPresentFlag = true, returnCode = ReturnCode.connectionAccepted))
        log.info("new connection establish : [{}]", self.path.name)
        invokePublish

      case p: PingReqPacket =>
        bridge ! MQTTOutboundPacket(p)

      case mqtt: PublishPacket => {
        context.actorOf(Props(classOf[InboundPublisher], sender, mqtt.fixedHeader.qos), {
          mqtt.fixedHeader.qos match {
            case 0 => inboundActorName(UUID.randomUUID().toString)
            case 1 => inboundActorName(mqtt.packetId.get.toString)
            case 2 => inboundActorName(mqtt.packetId.get.toString)
          }
        }) ! mqtt
      }

      case mqtt: PubRelPacket =>
        val actorName = inboundActorName(mqtt.packetId.toString)
        context.child(actorName) match {
          case Some(x) => x ! mqtt
          case None => log.error("[PUBREL] can't find publish inbound actor {}", actorName)
        }

      case mqtt: PubRecPacket =>
        val actorName = outboundActorName(mqtt.packetId.toString)
        context.child(actorName) match {
          case Some(x) => x ! mqtt
          case None => log.error("[PUBREC] can't find publish outbound actor {}", actorName)

        }

      case mqtt: PubAckPacket =>
        val actorName = outboundActorName(mqtt.packetId.toString)
        context.child(actorName) match {
          case Some(x) => x ! mqtt
          case None => log.error("[PUBACK] can't find publish outbound actor {} current child actors : {} packetId : {}", actorName,
            context.children.foldLeft(List[String]())((x, y) => {
              x :+ y.path.name
            }), mqtt.packetId)
        }

      case mqtt: PubCompPacket =>
        val actorName = outboundActorName(mqtt.packetId.toString)
        context.child(actorName) match {
          case Some(x) => x ! mqtt
          case None => log.error("[PUBCOMB] can't find publish outbound actor {} ", actorName)
        }

      case d: DisconnectPacket => {
        val currentConnectionStatus = connectionStatus
        connectionStatus = None
        currentConnectionStatus match {
          case Some(x) => x.destroyProperly
          case None =>
        }
        storage.socketClose
        context.children.foreach(child => {
          if (child.path.name.startsWith(PublishConstant.inboundPrefix) || child.path.name.startsWith(PublishConstant.outboundPrefix))
            context.stop(child)
        })
        log.info(" receive DISCONNECT : [{}]", self.path.name)

      }

      case subscribe: SubscribePacket =>
        subscribeTopics(subscribe)

      case unsubscribe: UnsubscribePacket =>
      // TODO : 이거 구현 필요.
      //        unsubscribeTopics(unsubscribe.topicFilter)
      //        sender ! MQTTOutboundPacket(UnsubAckPacket(packetId = unsubscribe.packetId))


    }

  }

  def subscribeTopics(subscribe: SubscribePacket) = {
    val session = self

    context.actorOf(SubscribeTopic.props(subscribe.topicFilter, session, connectionStatus, subscribe))
  }

  //  def unsubscribeTopics(topics: List[String]) = {
  //    val session = self
  //    topics.foreach(x => {
  //      SystemRoot.directoryProxy.tell(DirectoryTopicRequest(x), context.actorOf(Props(new Actor {
  //        def receive = {
  //          case DirectoryTopicResult(name, topicActors) =>
  //            topicActors.par.foreach(actor => actor ! Unsubscribe(session))
  //          //          topicActor != UNSUBSCRIBE
  //        }
  //      })))
  //
  //    }
  //    )
  //  }

  def invokePublish = {
    val session = self
    connectionStatus match {
      case Some(client) =>
        storage.nextMessage match {
          case Some(x) =>
            val actorName = PublishConstant.outboundPrefix + (x.packetId match {
              case Some(y) => y
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

object SubscribeTopic {
  def props(topicFilter: List[(String, Short)], session: ActorRef, connectionStatus: Option[ConnectionStatus], subscribe: SubscribePacket) = {
    Props(classOf[SubscribeTopic], topicFilter, session, connectionStatus, subscribe)
  }
}

class SubscribeTopic(topicFilter: List[(String, Short)],
                     session: ActorRef,
                     connectionStatus: Option[ConnectionStatus],
                     subscribe: SubscribePacket) extends Actor with ActorLogging {

  SystemRoot.topicManager ! Subscribe(topicFilter.map(x => TopicNameQos(x._1, x._2)), session)

  override def receive = {
    case response: Subscribed =>
      log.debug("[SUBSCRIBETOPIC] {}", response)
      connectionStatus match {
        case Some(x) => {
          log.debug("[SUBSCRIBETOPIC] send a result to {}", x)
          x.socket ! MQTTOutboundPacket(
            SubAckPacket(packetId = subscribe.packetId, returnCode = response.result)
          )
        }
        case None =>
      }
      context.stop(self)
  }
}