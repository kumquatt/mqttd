package plantae.citrus.mqtt.domains

import akka.util.ByteString
import plantae.citrus.mqtt._

object Connect {

  val MinValue = 0
  val MaxValue = 268435455

  def decode(bytes: ByteString) : CONNECT = {
    val fixedHeader = FixedHeader(ControlPacketType.CONNECT, false, QosLevel.AtLeastOnce, false, 0)
    val protocolName = "TET"
    val protocolVersion : Short = 1
    val cleanSession = false
    val keepAliveInSecodes = 10
    val clientId = "id"

    CONNECT(fixedHeader, protocolName, protocolVersion, cleanSession, keepAliveInSecodes, clientId)
  }
}

case class CONNECT(header: FixedHeader,
                   protocolName: String,
                   protocolVersion: Short,
                   cleanSession: Boolean,
                   keepAliveInSeconds: Int,
                   clientId: String,
                   username: Option[String] = None,
                   password: Option[String] = None,
                   willMessage: Option[Message] = None) extends ControlPacket

// TODO: need to implement
case object CONACK extends ControlPacket
