package plantae.citrus.mqtt.dto.connect

import plantae.citrus.mqtt.packet.ControlPacket
import scodec.Codec
import scodec.bits.{BitVector, ByteVector}

import scala.annotation.tailrec


case class Will(qos: Short, retain: Boolean, topic: String, message: ByteVector)

case class Authentication(id: String, password: Option[String])

case object ReturnCode {
  val connectionAccepted = 0.toShort
  val unacceptableProtocolVersion = 1.toShort
  val identifierRejected = 2.toShort
  val ServerUnavailable = 3.toShort
  val badUserNameOrPassword = 4.toShort
  val notAuthorized = 5.toShort
}

object PacketDecoder {


  def decode(data: BitVector): (List[ControlPacket], BitVector) = {

    @tailrec
    def decodeRec(data: BitVector, acc: List[ControlPacket]): (List[ControlPacket], BitVector) = {
      val result = Codec[ControlPacket].decode(data)
      if (result.isSuccessful) {
        if (result.require.remainder.nonEmpty) decodeRec(result.require.remainder, result.require.value :: acc)
        else (result.require.value :: acc, result.require.remainder)
      } else (acc, BitVector.empty)
    }

    decodeRec(data, List.empty[ControlPacket])

  }
}