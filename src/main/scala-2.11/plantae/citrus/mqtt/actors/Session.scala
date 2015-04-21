package plantae.citrus.mqtt.actors

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import plantae.citrus.mqtt.dto.Packet
import plantae.citrus.mqtt.dto.connect.{CONNACK, CONNECT, ReturnCode, Will}
import plantae.citrus.mqtt.dto.ping.{PINGREQ, PINGRESP}
import plantae.citrus.mqtt.dto.publish.PUBLISH

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

case class SessionCreation(connect: CONNECT, senderOfSender: ActorRef)

case class SessionCreationAck(connect: CONNECT, actor: ActorRef, senderOfSender: ActorRef)

class Session extends Actor {
  private val logger = Logging(context.system, this)
  var will = Option[Will](null)
  var keepAlive = 60000

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
      (ActorContainer.directory ! Register(connect.clientId.value, sender, senderOfSender, connect))
    }

    case SessionReset => {
      logger.info("session shutdown : " + self.toString())
      keepAlive = 60000
      will = None
      sender ! SessionResetAck
    }

    case SessionPingReq => {
      logger.info("session ping request")
      sender ! SessionPingResp
    }
  }

  def doMqttPacket(packet: Packet): Unit = {
    packet match {
      case connect: CONNECT =>
        logger.info("receive connect")
        will = connect.will
        keepAlive = connect.keepAlive.value

        sender ! MqttOutboundPacket(CONNACK(true, ReturnCode.connectionAccepted))
      case PINGREQ =>

        logger.info("receive pingreq")
        sender ! MqttOutboundPacket(PINGRESP)
      case publish: PUBLISH =>
    }

  }

  def doConnect(connect: CONNECT): CONNACK = {
    will = connect.will
    keepAlive = connect.keepAlive.value
    CONNACK(true, ReturnCode.connectionAccepted)
  }

}
