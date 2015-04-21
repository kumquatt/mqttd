package plantae.citrus.mqtt.actors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.io.Tcp.{Close, PeerClosed, Received, Write}
import akka.pattern.ask
import akka.util.ByteString
import com.google.common.base.Throwables
import plantae.citrus.mqtt.dto.PacketDecoder
import plantae.citrus.mqtt.dto.connect._
import plantae.citrus.mqtt.dto.ping._
import plantae.citrus.mqtt.dto.publish._
import plantae.citrus.mqtt.dto.subscribe.SUBSCRIBE
import plantae.citrus.mqtt.dto.unsubscribe.UNSUBSCRIBE

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
 * Created by yinjae on 15. 4. 21..
 */
class PacketBridge extends Actor {
  private val logger = Logging(context.system, this)
  implicit val timeout = akka.util.Timeout(5, TimeUnit.SECONDS)
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  var idx = 0
  var will = Option[Will](null)
  var keepAlive: Int = 1000 * 60
  var session: ActorRef = null
  var socket: ActorRef = null

  def receive = {

    case Result(connect, sess) => {
      logger.info("receive result " + self)
      this.session = sess
      this.session ! MqttInboundPacket(connect)
      logger.info("session : " + session)
    }
    case MqttOutboundPacket(packet) => {
      socket ! Write(ByteString(packet.encode))
    }
    case Received(data) => {
      PacketDecoder.decode(data.toArray[Byte]) match {
        case connect: CONNECT => {
          socket = sender
          doSession(connect)
        }
        case mqttPacket: PUBLISH => session ! MqttInboundPacket(mqttPacket)
        case mqttPacket: PUBACK => session ! MqttInboundPacket(mqttPacket)
        case mqttPacket: PUBREC => session ! MqttInboundPacket(mqttPacket)
        case mqttPacket: PUBREL => session ! MqttInboundPacket(mqttPacket)
        case mqttPacket: PUBCOMB => session ! MqttInboundPacket(mqttPacket)
        case mqttPacket: SUBSCRIBE => session ! MqttInboundPacket(mqttPacket)
        case mqttPacket: UNSUBSCRIBE => session ! MqttInboundPacket(mqttPacket)
        case PINGREQ => session ! MqttInboundPacket(PINGREQ)
        case DISCONNECT => session ! MqttInboundPacket(DISCONNECT)
      }
    }
    case PeerClosed => {
      session ! PeerClosed
      sender() ! Close
      context.stop(self)
    }

  }

  def doSession(connect: CONNECT) = {
    context.actorOf(Props[SessionChecker]) ! Get(connect, self)
  }


}

case class Get(connect: CONNECT, sender: ActorRef)

case class Result(connect: CONNECT, actorRef: ActorRef)

class SessionChecker extends Actor {
  private val logger = Logging(context.system, this)
  implicit val timeout = akka.util.Timeout(5, TimeUnit.SECONDS)
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  case class LoadResult(connect: CONNECT, actorRef: Option[ActorRef], sender: ActorRef)

  def receive = {
    case Get(connect, sender) => loadIfExist(connect, sender)
    case LoadResult(connect, options, sender) => options match {
      case Some(x) =>
        if (connect.cleanSession) {
          x ! SessionCommand(SessionShutdown)
          createSession(connect, sender)
        } else sender ! Result(connect, x)
      case None => createSession(connect, sender)
    }
    case SessionCreationAck(connect, session, sender) => {
      logger.info("receive session creation ack self" + self)
      logger.info("receive session creation ack sender" + sender)
      sender ! Result(connect, session)
    }
  }

  def loadIfExist(connect: CONNECT, sender: ActorRef) = {
    val clientId = connect.clientId.value
    (ActorContainer.directory ? DirectoryReq(clientId)) onComplete {
      case Success(DirectoryResp(name, option)) => option match {
        case Some(session) =>
          logger.info("find previous session " + session)
          (session ? SessionPingReq) onComplete {
            case Success(x) => {
              logger.info("previous session is valid " + session)
              self ! LoadResult(connect, Some(session), sender)
            }
            case Failure(t) =>
              logger.info("previous session is not valid " + Throwables.getStackTraceAsString(t))
              self ! LoadResult(connect, None, sender)
          }
        case None => {
          logger.info("can't previous session ")
          self ! LoadResult(connect, None, sender)

        }
      }


      case Failure(t) => self ! LoadResult(connect, None, sender)
    }
  }

  def createSession(connect: CONNECT, sender: ActorRef) = {
    logger.info("create new session " + connect.clientId.value)
    val session = context.actorOf(Props[Session], connect.clientId.value)
    session ! SessionCommand(SessionCreation(connect, sender))
  }
}

