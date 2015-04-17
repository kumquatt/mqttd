package plantae.citrus.mqtt.dto

/**
 * Created by yinjae on 15. 4. 17..
 */

case class MqttByteChunk(fixedHeaderBytes: Array[Byte], variableHeaderBytes: Array[Byte],
                         payloadBytes: Array[Byte])

object ByteChunkSplitter {
  def split(byteChunk: Array[Byte]): MqttByteChunk = {
    val remainLength = Decoder.decodeREMAININGLENGTH(byteChunk.slice(1, byteChunk.length))
    val variableHeaderLength = byteChunk(0) match {
      case ControlPacketType.CONNECT.value => 10
      case ControlPacketType.CONAACK.value => 2
      case ControlPacketType.PUBLISH.value => 7
      case ControlPacketType.PUBACK.value => 2
      case ControlPacketType.PUBREC.value => 2
      case ControlPacketType.PUBREL.value => 2
      case ControlPacketType.PUBCOMP.value => 2
      case ControlPacketType.SUBSCRIBE.value => 2
      case ControlPacketType.SUBACK.value => 2
      case ControlPacketType.UNSUBSCRIBE.value => 2
      case ControlPacketType.UNSUBACK.value => 2
      case ControlPacketType.PINGREQ.value => 2
      case ControlPacketType.PINGRESP.value => 2
      case ControlPacketType.DISCONNECT.value => 0
    }
    MqttByteChunk(byteChunk.slice(0, 1 + remainLength.usedByte), byteChunk.slice(1 + remainLength.usedByte, 1 + remainLength.usedByte + variableHeaderLength),
      byteChunk.slice(1 + remainLength.usedByte + variableHeaderLength, byteChunk.length)
    )
  }
}