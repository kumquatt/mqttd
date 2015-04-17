package plantae.citrus.mqtt.dto

trait PacketComponent {
  val usedByte: Int

  def encode: Array[Byte]
}

trait Packet extends PacketComponent {
  val serialize = fixedHeader.encode ++ variableHeader.encode ++ payload.encode

  def fixedHeader: FixedHeader

  def variableHeader: VariableHeader

  def payload: Payload

  override def encode: Array[Byte] = serialize
}


case class FixedHeader(packetType: BYTE, flag: List[BYTE], remainLegnth: REMAININGLENGTH) extends PacketComponent {
  val serialize = ((packetType | flag.foldLeft(BYTE(0x00))((r, c) => r | c)).encode ++ remainLegnth.encode).toArray

  override def encode: Array[Byte] = serialize

  override val usedByte: Int = encode.length
}

case class VariableHeader(headerElements: List[DataFormat]) extends PacketComponent {
  val serialize = headerElements.foldRight(List[Byte]())((each, accum) => each.encode ++ accum).toArray

  override def encode: Array[Byte] = serialize

  override val usedByte: Int = encode.length
}

case class Payload(payloadElements: List[DataFormat]) extends PacketComponent {
  val serialize = payloadElements.foldRight(List[Byte]())((each, accum) => each.encode ++ accum).toArray

  override def encode: Array[Byte] = serialize

  override val usedByte: Int = encode.length
}



object EMPTY_COMPONENT {
  val EMPTY_VARIABLE_HEADER = VariableHeader(List())
  val EMPTY_PAYLOAD = Payload(List())
}

class ControlPacketType

object ControlPacketType {
  val RESERVED_0 = BYTE((0x0 << 4).toByte)
  val CONNECT = BYTE((0x1 << 4).toByte)
  val CONAACK = BYTE((0x2 << 4).toByte)
  val PUBLISH = BYTE((0x3 << 4).toByte)
  val PUBACK = BYTE((0x4 << 4).toByte)
  val PUBREC = BYTE((0x5 << 4).toByte)
  val PUBREL = BYTE((0x6 << 4).toByte)
  val PUBCOMP = BYTE((0x7 << 4).toByte)
  val SUBSCRIBE = BYTE((0x8 << 4).toByte)
  val SUBACK = BYTE((0x9 << 4).toByte)
  val UNSUBSCRIBE = BYTE((0xa << 4).toByte)
  val UNSUBACK = BYTE((0xb << 4).toByte)
  val PINGREQ = BYTE((0xc << 4).toByte)
  val PINGRESP = BYTE((0xd << 4).toByte)
  val DISCONNECT = BYTE((0xe << 4).toByte)
  val RESERVED_F = BYTE((0xf << 4).toByte)
}

class ControlPacketFlag

object ControlPacketFlag {
  val RESERVED_0 = BYTE(0x0)
  val CONNECT = BYTE(0x0)
  val PUBLISH_DUP = BYTE((0x1 << 3).toByte)
  val PUBLISH_QOS_0 = BYTE((0x0 << 1).toByte)
  val PUBLISH_QOS_1 = BYTE((0x1 << 1).toByte)
  val PUBLISH_QOS_2 = BYTE((0x2 << 1).toByte)
  val PUBLISH_RETAIN = BYTE(0x1)
  val PUBACK = BYTE(0x0)
  val PUBREC = BYTE(0x0)
  val PUBREL = BYTE((0x1 << 1).toByte)
  val PUBCOMP = BYTE(0x0)
  val SUBSCRIBE = BYTE((0x1 << 1).toByte)
  val SUBACK = BYTE(0x0)
  val UNSUBSCRIBE = BYTE((0x1 << 1).toByte)
  val UNSUBACK = BYTE(0x0)
  val PINGREQ = BYTE(0x0)
  val PINGRESP = BYTE(0x0)
  val DISCONNECT = BYTE(0x0)
}


