package plantae.citrus.mqtt

trait ControlPacket

object ControlPacketType extends Enumeration {

  val R0 = Value(0) // Reserved
  val CONNECT = Value(1)
  val CONAACK = Value(2)
  val PUBLISH = Value(3)
  val PUBACK = Value(4)
  val PUBREC = Value(5)
  val PUBREL = Value(6)
  val PUBCOMP = Value(7) // Publish Complete
  val SUBSCRIBE = Value(8)
  val SUBACK = Value(9)
  val UNSUBSCRIBE = Value(10)
  val UNSUBACK = Value(11)
  val PINGREQ = Value(12)
  val PINGRESP = Value(13)
  val DISCONNECT = Value(14)
  val R15 = Value(15) // Reserved

}

object ConnectReturnCode extends Enumeration {
  val Accepted = Value(0)
  val UnacceptableProtocolVersion = Value(1) // Connection Refused: unacceptable protocol version
  val IdentifierRejected = Value(2) // Connection Refused: identifier rejected
  val ServerUnavailable = Value(3) // Connection Refused: server unavailable
  val BadUserOrPassword = Value(4) // Connection Refused: bad user name or password
  val NotAuthorized = Value(5) // Connection Refused: not authorized
}

object QosLevel extends Enumeration {
  val AtMostOnce = Value(0)
  val AtLeastOnce = Value(1)
  val ExactlyOnce = Value(2)
  val R3 = Value(3) // Reserved
}