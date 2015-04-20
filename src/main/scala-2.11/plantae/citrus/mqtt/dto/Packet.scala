package plantae.citrus.mqtt.dto

trait PacketComponent {
  def usedByte: Int

  def encode: Array[Byte]
}

trait Packet extends PacketComponent {
  def fixedHeader: FixedHeader

  def variableHeader: VariableHeader

  def payload: Payload

  override def encode: Array[Byte] = fixedHeader.encode ++ variableHeader.encode ++ payload.encode
}


case class FixedHeader(packetType: BYTE, remainLegnth: REMAININGLENGTH) extends PacketComponent {

  override def encode: Array[Byte] = (packetType.encode ++ remainLegnth.encode).toArray

  override def usedByte: Int = encode.length
}

case class VariableHeader(headerElements: List[DataFormat]) extends PacketComponent {

  override def encode: Array[Byte] = headerElements.foldRight(List[Byte]())((each, accum) => each.encode ++ accum).toArray

  override def usedByte: Int = encode.length
}

case class Payload(payloadElements: List[DataFormat]) extends PacketComponent {

  override def encode: Array[Byte] = payloadElements.foldRight(List[Byte]())((each, accum) => each.encode ++ accum).toArray

  override def usedByte: Int = encode.length
}

// FIXME : it is useful but not look nice, How about using option for VariableHeader, Payload
case object EMPTY_COMPONENT {
  val EMPTY_VARIABLE_HEADER = VariableHeader(List())
  val EMPTY_PAYLOAD = Payload(List())
}

class ControlPacketType

case object ControlPacketType {
  val RESERVED_0 = BYTE(0x00) << 4
  val CONNECT = BYTE(0x1) << 4
  val CONNACK = BYTE(0x2) << 4
  val PUBLISH = BYTE(0x3) << 4
  val PUBACK = BYTE(0x4) << 4
  val PUBREC = BYTE(0x5) << 4
  val PUBREL = BYTE(0x6) << 4
  val PUBCOMP = BYTE(0x7) << 4
  val SUBSCRIBE = BYTE(0x8) << 4
  val SUBACK = BYTE(0x9) << 4
  val UNSUBSCRIBE = BYTE(0xa) << 4
  val UNSUBACK = BYTE(0xb) << 4
  val PINGREQ = BYTE(0xc) << 4
  val PINGRESP = BYTE(0xd) << 4
  val DISCONNECT = BYTE(0xe) << 4
  val RESERVED_F = BYTE(0xf) << 4
}

