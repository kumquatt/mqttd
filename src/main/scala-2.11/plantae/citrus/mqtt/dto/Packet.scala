package plantae.citrus.mqtt.dto

import plantae.citrus.mqtt.dto.Decoder.ByteStream
import plantae.citrus.mqtt.dto.connect._
import plantae.citrus.mqtt.dto.ping.PINGREQDecoder
import plantae.citrus.mqtt.dto.publish._
import plantae.citrus.mqtt.dto.subscribe.SUBSCRIBEDecoder
import plantae.citrus.mqtt.dto.unsubscribe.UNSUBSCRIBEDecoder

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
  val EMPTY_FIXED_HEADER = FixedHeader(BYTE(0xFF.toByte), REMAININGLENGTH(0))

  val EMPTY_VARIABLE_HEADER = VariableHeader(List())
  val EMPTY_PAYLOAD = Payload(List())
}

class ControlPacketType

case object ControlPacketType {
  val RESERVED_0 = BYTE(0x00)
  val CONNECT = BYTE(0x10)
  val CONNACK = BYTE(0x20)
  val PUBLISH = BYTE(0x30)
  val PUBACK = BYTE(0x40)
  val PUBREC = BYTE(0x50)
  val PUBREL = BYTE(0x60)
  val PUBCOMP = BYTE(0x70)
  val SUBSCRIBE = BYTE(0x80.toByte)
  val SUBACK = BYTE(0x90.toByte)
  val UNSUBSCRIBE = BYTE(0xa0.toByte)
  val UNSUBACK = BYTE(0xb0.toByte)
  val PINGREQ = BYTE(0xc0.toByte)
  val PINGRESP = BYTE(0xd0.toByte)
  val DISCONNECT = BYTE(0xe0.toByte)
  val RESERVED_F = BYTE(0xf0.toByte)
}

object PacketDecoder {

  def decode(data: Array[Byte]): (List[Packet], Array[Byte]) = {
    if (data.length < 4) (List(), data)
    else {
      val byteStream = ByteStream(data)
      Decoder.decodeBYTE(byteStream)
      val remainingBytes = Decoder.decodeREMAININGLENGTH(byteStream)
      val bytesForPacket = (1 + remainingBytes.usedByte + remainingBytes.value)
      if (bytesForPacket <= data.length) {
        val rest = data.drop(1 + remainingBytes.usedByte + remainingBytes.value)
        val packet = decodePacket(data.take(1 + remainingBytes.usedByte + remainingBytes.value))
        val restPacket = decode(rest)
        (List(packet) ++ restPacket._1, restPacket._2)
      } else {
        (List(), data)
      }
    }

  }

  def decodePacket(data: Array[Byte]): Packet = {

    val packet = BYTE(data(0)) & BYTE(0xF0.toByte) match {
      case ControlPacketType.CONNECT => CONNECTDecoder.decode(data)
      case ControlPacketType.PINGREQ => PINGREQDecoder.decode(data)
      case ControlPacketType.DISCONNECT => DISCONNECTDecoder.decode(data)
      case ControlPacketType.PUBLISH => PUBLISHDecoder.decode(data)
      case ControlPacketType.PUBACK => PUBACKDecoder.decode(data)
      case ControlPacketType.PUBREC => PUBRECDecoder.decode(data)
      case ControlPacketType.PUBREL => PUBRELDecoder.decode(data)
      case ControlPacketType.PUBCOMP => PUBCOMBDecoder.decode(data)
      case ControlPacketType.SUBSCRIBE => SUBSCRIBEDecoder.decode(data)
      case ControlPacketType.UNSUBSCRIBE => UNSUBSCRIBEDecoder.decode(data)
    }
    packet
  }
}