package plantae.citrus.mqtt.domains

import plantae.citrus.mqtt.{ControlPacket, FixedHeader, Message}

case class CONNECT(header: FixedHeader,
                   protocolName: String,
                   protocolVersion: Short,
                   cleanSession: Boolean,
                   keepAliveInSeconds: Int,
                   clientId: String,
                   username: Option[String],
                   password: Option[String],
                   willMessage: Option[Message]) extends ControlPacket

// TODO: need to implement
case object CONACK extends ControlPacket
