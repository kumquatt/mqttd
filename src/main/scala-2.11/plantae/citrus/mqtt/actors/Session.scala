package plantae.citrus.mqtt.actors

import java.util.concurrent.TimeUnit

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

case class MqttInboundPacket(mqttPacket: Packet)

case class MqttOutboundPacket(mqttPacket: Packet)

case class SessionCommand(command: AnyRef)

case object SessionPingReq

case object SessionPingResp

case object SessionReset

case object SessionResetAck

case object SessionKeepaliveTimeOut

case class SessionCreation(connect: CONNECT, senderOfSender: ActorRef)

case class SessionCreationAck(connect: CONNECT, actor: ActorRef, senderOfSender: ActorRef)

class Session extends Actor {
  implicit val timeout = akka.util.Timeout(5, TimeUnit.SECONDS)
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  import ActorContainer.system.dispatcher

  private val logger = Logging(context.system, this)
  var will = Option[Will](null)
  var keepAlive = 60
  var keepAliveTimer: Cancellable = null
  var clientId: String = null

  override def postStop = {
    logger.info("post stop - shutdown session")
  }

  override def receive: Receive = {
    case MqttInboundPacket(mqttPacket) => doMqttPacket(mqttPacket)
    case SessionCommand(command) => doSessionCommand(command)
    case RegisterAck(name, sender, senderOfSender, connect) => {
      logger.info("receive register ack")
      sender ! SessionCreationAck(connect, self, senderOfSender)
    }
    case everythingElse => println(everythingElse)
  }

  def doSessionCommand(command: AnyRef): Unit = command match {

    case SessionCreation(connect, senderOfSender) => {
      logger.info("session create : " + self.toString())
      clientId = connect.clientId.value
      (ActorContainer.directory ! Register(connect.clientId.value, sender, senderOfSender, connect))
    }

    case SessionReset => {
      logger.info("session shutdown : " + self.toString())
      keepAlive = 60
      will = None
      sender ! SessionResetAck
    }

    case SessionPingReq => {
      logger.info("session ping request")
      sender ! SessionPingResp
    }

    case SessionKeepaliveTimeOut => {
      logger.info("No keep alive request!!!!")
    }
  }

  def doMqttPacket(packet: Packet): Unit = {
    packet match {
      case connect: CONNECT =>
        logger.info("receive connect")
        will = connect.will
        keepAlive = connect.keepAlive.value

        logger.info("Keepalive time is {}", keepAlive)
        sender ! MqttOutboundPacket(CONNACK(true, ReturnCode.connectionAccepted))
        keepAliveTimer = ActorContainer.system.scheduler.scheduleOnce(keepAlive second, self, SessionCommand(SessionKeepaliveTimeOut))
      case PINGREQ =>

        if (keepAliveTimer != null) {
          logger.info("Cancel the keepalivetimer and reset")
          keepAliveTimer.cancel()
        }
        keepAliveTimer = ActorContainer.system.scheduler.scheduleOnce(keepAlive second, self, SessionCommand(SessionKeepaliveTimeOut))

        logger.info("receive pingreq")
        sender ! MqttOutboundPacket(PINGRESP)
      case publish: PUBLISH =>

      case subscribe: SUBSCRIBE =>
        subscribeToTopics(subscribe.topicFilter)


      case unsubscribe: UNSUBSCRIBE =>
        unsubscribeToTopics(unsubscribe.topicFilter)
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
    will = connect.will
    keepAlive = connect.keepAlive.value
    CONNACK(true, ReturnCode.connectionAccepted)
  }

}
