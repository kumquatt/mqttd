package plantae.citrus.mqtt.dto.subscribe

import plantae.citrus.mqtt.dto.Decoder.ByteStream
import plantae.citrus.mqtt.dto._

/**
 * Created by yinjae on 15. 4. 20..
 */
case class SUBSCRIBE(packetId: INT, topicFilter: List[TopicFilter]) extends Packet {
  override def fixedHeader: FixedHeader = FixedHeader(BYTE(0x08) << 4, REMAININGLENGTH(variableHeader.usedByte + payload.usedByte))

  override def variableHeader: VariableHeader = VariableHeader(List(packetId))

  override def payload: Payload = Payload(topicFilter.foldRight(List[DataFormat]())((filter, accum) => filter.topic :: filter.qos :: accum))

  override def usedByte: Int = encode.length
}

case class TopicFilter(topic: STRING, qos: BYTE)

object SUBSCRIBEDecoder {
  def decode(bytes: Array[Byte]): SUBSCRIBE = {
    val stream = ByteStream(bytes)
    val typeAndFlag = Decoder.decodeBYTE(stream)

    if (typeAndFlag != (BYTE(0x08) << 4))
      throw new Error

    val remainingLength = Decoder.decodeREMAININGLENGTH(stream)
    val packetId = Decoder.decodeINT(stream)

    val endPosition = remainingLength.value + remainingLength.usedByte + typeAndFlag.usedByte

    def extractTopicFilter: List[TopicFilter] = {
      if (stream.position == endPosition)
        List()
      else {
        TopicFilter(Decoder.decodeSTRING(stream), Decoder.decodeBYTE(stream)) :: extractTopicFilter
      }
    }
    SUBSCRIBE(packetId, extractTopicFilter)
  }
}

case class SUBACK(packetId: INT, returnCode: List[BYTE]) extends Packet {
  override def fixedHeader: FixedHeader = FixedHeader(BYTE(0x09) << 4, REMAININGLENGTH(variableHeader.usedByte + payload.usedByte))

  override def variableHeader: VariableHeader = VariableHeader(List(packetId))

  override def payload: Payload = Payload(returnCode)

  override def usedByte: Int = encode.length
}


object SUBACKDecoder {
  def decode(bytes: Array[Byte]): SUBACK = {
    val stream = ByteStream(bytes)
    val typeAndFlag = Decoder.decodeBYTE(stream)

    if (typeAndFlag != (BYTE(0x09) << 4))
      throw new Error

    val remainingLength = Decoder.decodeREMAININGLENGTH(stream)
    val packetId = Decoder.decodeINT(stream)

    val endPosition = remainingLength.value + remainingLength.usedByte + typeAndFlag.usedByte

    def subAckReturnCode: List[BYTE] = {
      if (stream.position == endPosition)
        List()
      else {
        Decoder.decodeBYTE(stream) :: subAckReturnCode
      }
    }
    SUBACK(packetId, subAckReturnCode)
  }
}
