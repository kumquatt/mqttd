package plantae.citrus.mqtt.dto

import java.util.concurrent.atomic.AtomicInteger
import java.lang.{Short => JavaShort, Integer => JavaInt}
trait DataFormat {
  val encode: List[Byte]

  val usedByte: Int
}

case class BYTE(value: Byte) extends DataFormat {

  def isMostSignificantBitOn: Boolean = value < 0

  def isLeastSignificantBitOn: Boolean = ((value & 0x01.toByte)) > 0

  val hexa: String = {
    val hexaString = Integer.toHexString(value.toInt)
    if (hexaString.length > 2) {
      "0x" + hexaString.substring(hexaString.length - 2, hexaString.length).toUpperCase
    } else "0x" + hexaString.toUpperCase

  }

  def |(x: BYTE): BYTE = this.|(x.value)

  def |(x: Byte): BYTE = BYTE((value | x).toByte)

  def &(x: BYTE): BYTE = this.&(x.value)

  def &(x: Byte): BYTE = BYTE((value & x).toByte)

  def <<(x: INT): BYTE = this.<<(x.value)

  def >>(x: INT): BYTE = this.>>(x.value)

  def <<(x: Int): BYTE = BYTE((value << x).toByte)

  def >>(x: Int): BYTE = BYTE((value >> x).toByte)


  override val encode: List[Byte] = {
    List(value)
  }

  override val usedByte: Int = 1

  def toBoolean: Boolean = value != 0


  def toINT: INT = {
    INT(value.toShort)
  }
}

case class INT(value: Short) extends DataFormat {
  def mostSignificantByte: BYTE = BYTE(((value & 0xFF00) >> 8).toByte)

  def leastSignificantByte: BYTE = BYTE((value & 0x00FF).toByte)

  def this(msb: BYTE, lsb: BYTE) {
    this(((msb.value) << 8 & 0xFF00 | lsb.value).toShort)
  }

  override val encode: List[Byte] = {
    List(mostSignificantByte.value, leastSignificantByte.value)
  }

  override val usedByte: Int = 2

  def <(x: INT): Boolean = this.<(x.value)

  def >(x: INT): Boolean = this.>(x.value)

  def ==(x: INT): Boolean = this.==(x.value)

  def <(x: Int): Boolean = value < x

  def >(x: Int): Boolean = value > x

  def ==(x: Int): Boolean = value == x

  def toBYTE: BYTE = BYTE(value.toByte)
  def toHexa = {
    JavaInt.toHexString(value)
  }
}

case class REMAININGLENGTH(value: Int) extends DataFormat {

  def remainingLengthEncoding(x: Int): List[Byte] = {

    if (x <= 0) List()
    else {
      val newX = x / 128
      def continueBitOn = if (newX > 0) 128 else 0
      (x % 128 | continueBitOn).toByte :: remainingLengthEncoding(newX)
    }
  }

  override val encode: List[Byte] = remainingLengthEncoding(value)

  override val usedByte: Int = encode.length
}

case class STRING(value: String) extends DataFormat {
  val length: Short = value.getBytes.length.toShort

  override val encode: List[Byte] = {
    INT(length).encode ++ value.getBytes.toList
  }

  override val usedByte: Int = encode.length
}

case class PUBLISHPAYLOAD(value: Array[Byte]) extends DataFormat {


  override val encode: List[Byte] = value.toList


  override val usedByte: Int = encode.length
}

object Encoder {
  def encode(source: Option[DataFormat]): List[Byte] = source match {
    case None => List()
    case Some(x) => x match {
      case str: STRING => Encoder.encode(Some(INT(str.length))) ++ str.value.getBytes.toList
      case int: INT => List(int.mostSignificantByte.value, int.leastSignificantByte.value)
      case byte: BYTE => List(byte.value)
      case remainingLength: REMAININGLENGTH => {
        def remainingLengthEncoding(x: Int): List[Byte] = {
          if (x <= 0) List()
          else {
            val newX = x / 128
            val continueBitOn = if (newX > 0) 128 else 0
            (x % 128 | continueBitOn).toByte :: remainingLengthEncoding(newX)
          }
        }
        remainingLengthEncoding(remainingLength.value)
      }
    }
  }
}

object Decoder {

  case class ByteStream(bytes: Array[Byte]) {
    val pos = new AtomicInteger(0)
    val length = bytes.length

    def proceed(delta: DataFormat): Unit = pos.addAndGet(delta.usedByte)

    def rest = bytes.slice(pos.get(), length)

    def position: INT = INT(pos.get().toShort)
  }

  def decodeREMAININGLENGTH(stream: ByteStream): REMAININGLENGTH = {
    val data = REMAININGLENGTH(remainingLengthDecoding(stream.rest, 0, 1))
    stream.proceed(data)
    data
  }

  def decodeBYTE(stream: ByteStream): BYTE = {
    val data = BYTE(stream.rest(0))
    stream.proceed(data)
    data
  }

  def decodeINT(stream: ByteStream): INT = {
    val data = INT(((stream.rest(0) << 8 & 0xFF00) | (stream.rest(1) & 0x00FF)).toShort)
    stream.proceed(data)
    data
  }


  def decodeSTRING(stream: ByteStream): STRING = {
    val length = (stream.rest(0) << 8 & 0xFF00 | stream.rest(1)).toShort
    val data = STRING(new String(stream.rest.slice(2, 2 + length)))
    stream.proceed(data)
    data
  }

  def decodePUBLISHPAYLOAD(stream: ByteStream, size: Int): PUBLISHPAYLOAD = {
    val data = PUBLISHPAYLOAD(stream.rest.slice(0, size))
    stream.proceed(data)
    data
  }


  private def remainingLengthDecoding(bytes: Array[Byte], index: Int, multiplier: Int): Int = {
    val encodeByte = bytes(index)
    val newValue = (encodeByte & 127) * multiplier
    if ((encodeByte & 128) == 0) newValue
    else newValue + remainingLengthDecoding(bytes, index + 1, multiplier * 128)
  }
}