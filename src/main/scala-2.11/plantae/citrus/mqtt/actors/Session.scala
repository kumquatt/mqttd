package plantae.citrus.mqtt.actors

import java.util.concurrent.TimeUnit

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

  private val logger = Logging(context.system, this)

  import ActorContainer.system.dispatcher

  case class ConnectionStatus(will: Option[Will], keepAliveTime: Int)

  var connectionStatus: Option[ConnectionStatus] = None
  var keepAliveTimer: Option[Cancellable] = None

  override def preStart = {
    ActorContainer.directory ! Register(self.path.name)
  }

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

  def session(command: AnyRef): Unit = command match {

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
      case DISCONNECT => {
        cancelTimer
      }
    }

  }

  def doConnect(connect: CONNECT): CONNACK = {
    CONNACK(true, ReturnCode.connectionAccepted)
  }
}
