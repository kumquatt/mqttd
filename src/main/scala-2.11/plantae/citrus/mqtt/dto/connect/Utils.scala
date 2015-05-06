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


  def decode(data: Array[Byte]): (List[ControlPacket], Array[Byte]) = {

    @tailrec
    def decodeRec(data: Array[Byte], acc: List[ControlPacket]) : (List[ControlPacket], Array[Byte]) = {
      val result = Codec[ControlPacket].decode(BitVector(data))

      if (result.isSuccessful && result.require.remainder.nonEmpty) decodeRec(result.require.remainder.toByteArray, result.require.value :: acc)
      else if (result.isSuccessful) (result.require.value :: acc, result.require.remainder.toByteArray)
      else (acc, Array.empty[Byte])
    }

    val a = decodeRec(data, List.empty[ControlPacket])

    a
  }
}