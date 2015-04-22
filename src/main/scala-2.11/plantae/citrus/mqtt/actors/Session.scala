package plantae.citrus.mqtt.actors

import java.util.concurrent.TimeUnit

<<<<<<< HEAD
import akka.actor.{Actor, ActorRef, Cancellable, Props}
import akka.event.Logging
import plantae.citrus.mqtt.dto.Packet
import plantae.citrus.mqtt.dto.connect._
import plantae.citrus.mqtt.dto.ping.{PINGREQ, PINGRESP}
import plantae.citrus.mqtt.dto.publish.PUBLISH

import scala.concurrent.duration.FiniteDuration

class SessionCreator extends Actor {
  override def receive = {
    case clientId: String => sender ! context.actorOf(Props[Session], clientId)
  }
}
=======
import akka.actor.{Actor, ActorRef, Cancellable}
import akka.event.Logging
import akka.pattern.ask
import plantae.citrus.mqtt.dto.connect.{CONNACK, CONNECT, ReturnCode, Will}
import plantae.citrus.mqtt.dto.ping.{PINGREQ, PINGRESP}
import plantae.citrus.mqtt.dto.publish.PUBLISH
import plantae.citrus.mqtt.dto.subscribe.{SUBSCRIBE, TopicFilter}
import plantae.citrus.mqtt.dto.unsubscribe.UNSUBSCRIBE
import plantae.citrus.mqtt.dto.{Packet, STRING}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * Created by yinjae on 15. 4. 21..
 */
>>>>>>> 81c10681884e05d8005978aad404136e67516114

case class MqttInboundPacket(mqttPacket: Packet)

case class MqttOutboundPacket(mqttPacket: Packet)

case class SessionCommand(command: AnyRef)

case object SessionPingReq

case object SessionPingResp

case object SessionReset

case object SessionResetAck

case object SessionKeepAliveTimeOut

case object ConnectionClose

class Session extends Actor {
<<<<<<< HEAD

  private val logger = Logging(context.system, this)

  import ActorContainer.system.dispatcher

  case class ConnectionStatus(will: Option[Will], keepAliveTime: Int)

  var connectionStatus: Option[ConnectionStatus] = None
  var keepAliveTimer: Option[Cancellable] = None

  override def preStart = {
    ActorContainer.directory ! Register(self.path.name)
  }
=======
  implicit val timeout = akka.util.Timeout(5, TimeUnit.SECONDS)
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  import ActorContainer.system.dispatcher

  private val logger = Logging(context.system, this)
  var will = Option[Will](null)
  var keepAlive = 60
  var keepAliveTimer: Cancellable = null
  var clientId: String = null
>>>>>>> 81c10681884e05d8005978aad404136e67516114

  override def postStop = {
    ActorContainer.directory ! Remove(self.path.address.toString)
    logger.info("post stop - shutdown session")
  }

  override def receive: Receive = {
    case MqttInboundPacket(packet) => mqttPacket(packet, sender)
    case SessionCommand(command) => session(command)
    case RegisterAck(name) => {
      logger.info("receive register ack")
    }
    case everythingElse => println(everythingElse)
  }

<<<<<<< HEAD
  def session(command: AnyRef): Unit = command match {
=======
  def doSessionCommand(command: AnyRef): Unit = command match {

    case SessionCreation(connect, senderOfSender) => {
      logger.info("session create : " + self.toString())
      clientId = connect.clientId.value
      (ActorContainer.directory ! Register(connect.clientId.value, sender, senderOfSender, connect))
    }
>>>>>>> 81c10681884e05d8005978aad404136e67516114

    case SessionReset => {
      logger.info("session reset : " + self.toString())
      connectionStatus = None
      sender ! SessionResetAck
    }

    case SessionPingReq => {
      logger.info("session ping request")
      sender ! SessionPingResp
    }

    case SessionKeepAliveTimeOut => {
      logger.info("No keep alive request!!!!")
    }
    case ConnectionClose => {
      //TODO : will process handling
      connectionStatus = None
    }

  }

  def cancelTimer = {
    keepAliveTimer match {
      case Some(x) => if (x.isCancelled) x.cancel()
      case None =>
    }
  }

  def resetTimer = {
    cancelTimer
    connectionStatus match {
      case Some(x) =>
        keepAliveTimer = Some(ActorContainer.system.scheduler.scheduleOnce(
          FiniteDuration(x.keepAliveTime, TimeUnit.SECONDS), self, SessionCommand(SessionKeepAliveTimeOut))
        )
      case None =>
    }
  }

  def mqttPacket(packet: Packet, bridge: ActorRef): Unit = {

    packet match {
      case connect: CONNECT =>
        logger.info("receive connect")
        connectionStatus = Some(ConnectionStatus(connect.will, connect.keepAlive.value))
        bridge ! MqttOutboundPacket(CONNACK(true, ReturnCode.connectionAccepted))
        resetTimer
      case PINGREQ =>
        resetTimer
        logger.info("receive pingreq")
        bridge ! MqttOutboundPacket(PINGRESP)
      case publish: PUBLISH =>
<<<<<<< HEAD
      case DISCONNECT => {
        cancelTimer
      }
=======

      case subscribe: SUBSCRIBE =>
        subscribeToTopics(subscribe.topicFilter)


      case unsubscribe: UNSUBSCRIBE =>
        unsubscribeToTopics(unsubscribe.topicFilter)
>>>>>>> 81c10681884e05d8005978aad404136e67516114
    }

  }

  def subscribeToTopics(topicFilters: List[TopicFilter]) = {
    topicFilters.foreach(tp =>
      (ActorContainer.topicDirectory ? TopicDirectoryReq(tp.topic)) onComplete {
        case Success(TopicDirectoryResp(topicName, option: Option[ActorRef])) => option match {
          case Some(topicActor) => {
            logger.info("I will subscribe to actor({}) topicName({}) clientId({})",
              topicActor, tp.topic.value, clientId
            )
            topicActor ! Subscribe(clientId)
          }
          case None => {
            logger.info("No topic actor topicName({}) clientId({})", tp.topic.value, clientId)
          }
        }
        case Failure(t) => {
          logger.info("Ask failure topicName({}) clientId({})", tp.topic.value, clientId)
          None
        }
      }

    )

  }

  def unsubscribeToTopics(topics: List[STRING]) = {
    topics.foreach(tp =>
      (ActorContainer.topicDirectory ? TopicDirectoryReq(tp)) onComplete {
        case Success(TopicDirectoryResp(topicName, option: Option[ActorRef])) => option match {
          case Some(topicActor) => {
            logger.info("I will unsubscribe to actor({}) topicName({}) clientId({})",
              topicActor, tp.value, clientId
            )
            topicActor ! Unsubscribe(clientId)
          }
          case None => {
            logger.info("No topic actor topicName({}) clientId({})", tp.value, clientId)
          }
        }
        case Failure(t) => {
          logger.info("Ask failure topicName({}) clientId({})", tp.value, clientId)
          None
        }
      }
    )
  }

  def doConnect(connect: CONNECT): CONNACK = {
    CONNACK(true, ReturnCode.connectionAccepted)
  }
}
