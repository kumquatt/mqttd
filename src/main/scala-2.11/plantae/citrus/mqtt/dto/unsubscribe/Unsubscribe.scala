package plantae.citrus.mqtt.dto.unsubscribe

import plantae.citrus.mqtt.dto.Decoder.ByteStream
import plantae.citrus.mqtt.dto._

/**
 * Created by yinjae on 15. 4. 20..
 */
case class UNSUBSCRIBE(packetId: INT, topicFilter: List[STRING]) extends Packet {
  override def fixedHeader: FixedHeader = FixedHeader(BYTE(0x0a) << 4 | BYTE(0x02), REMAININGLENGTH(variableHeader.usedByte + payload.usedByte))

  override def variableHeader: VariableHeader = VariableHeader(List(packetId))

  override def payload: Payload = Payload(topicFilter)

  override def usedByte: Int = encode.length
}


object UNSUBSCRIBEDecoder {
  def decode(bytes: Array[Byte]): UNSUBSCRIBE = {
    val stream = ByteStream(bytes)
    val typeAndFlag = Decoder.decodeBYTE(stream)

    if (typeAndFlag != (BYTE(0x0a) << 4 | BYTE(0x02)))
      throw new Error

    val remainingLength = Decoder.decodeREMAININGLENGTH(stream)
    val packetId = Decoder.decodeINT(stream)

    val endPosition = remainingLength.value + remainingLength.usedByte + typeAndFlag.usedByte

    def extractTopicFilter: List[STRING] = {
      if (stream.position == endPosition)
        List()
      else {
        Decoder.decodeSTRING(stream) :: extractTopicFilter
      }
    }
    UNSUBSCRIBE(packetId, extractTopicFilter)
  }
}

case class UNSUBACK(packetId: INT) extends Packet {
  override def fixedHeader: FixedHeader = FixedHeader(BYTE(0x0b) << 4, REMAININGLENGTH(variableHeader.usedByte + payload.usedByte))

  override def variableHeader: VariableHeader = VariableHeader(List(packetId))

  override def payload: Payload = EMPTY_COMPONENT.EMPTY_PAYLOAD

  override def usedByte: Int = encode.length
}


object UNSUBACKDecoder {
  def decode(bytes: Array[Byte]): UNSUBACK = {
    val stream = ByteStream(bytes)
    val typeAndFlag = Decoder.decodeBYTE(stream)

    if (typeAndFlag != (BYTE(0x0b) << 4))
      throw new Error

    val remainingLength = Decoder.decodeREMAININGLENGTH(stream)
    val packetId = Decoder.decodeINT(stream)

    UNSUBACK(packetId)
  }
}
