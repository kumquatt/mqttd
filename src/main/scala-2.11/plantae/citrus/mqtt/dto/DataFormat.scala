package plantae.citrus.mqtt.dto

import java.util.concurrent.atomic.AtomicInteger

trait DataFormat {
  def encode: List[Byte]

  def usedByte: Int
}


case class BYTE(value: Byte) extends DataFormat {

  def isMostSignificantBitOn: Boolean = value < 0

  def isLeastSignificantBitOn: Boolean = ((value & 0x01.toByte)) > 0

  def hexa: String = {
    val hexaString = Integer.toHexString(value.toInt)
    if (hexaString.length > 2) {
      "0x" + hexaString.substring(hexaString.length - 2, hexaString.length).toUpperCase
    } else "0x" + hexaString.toUpperCase

  }

  def |(x: BYTE): BYTE = BYTE((value | x.value).toByte)

  def &(x: BYTE): BYTE = BYTE((value & x.value).toByte)

  def <<(x: INT): BYTE = this.<<(x.value)

  def >>(x: INT): BYTE = this.>>(x.value)

  def <<(x: Int): BYTE = BYTE((value << x).toByte)

  def >>(x: Int): BYTE = BYTE((value >> x).toByte)


  override def encode: List[Byte] = {
    List(value)
  }

  override def usedByte: Int = 1

  def toBoolean: Boolean = {
    return value != 0
  }
}

case class INT(value: Short) extends DataFormat {
  def mostSignificantByte: BYTE = BYTE(((value & 0xFF00) >> 8).toByte)

  def leastSignificantByte: BYTE = BYTE((value & 0x00FF).toByte)

  def this(msb: BYTE, lsb: BYTE) {
    this(((msb.value) << 8 & 0xFF00 | lsb.value).toShort)
  }


  override def encode: List[Byte] = {
    List(mostSignificantByte.value, leastSignificantByte.value)
  }

  override def usedByte: Int = 2
}

case class REMAININGLENGTH(value: Int) extends DataFormat {

  def remainingLengthEncoding(x: Int): List[Byte] = {

    if (x <= 0)
      List()
    else {
      val newX = x / 128
      def continueBitOn = if (newX > 0) 128 else 0
      (x % 128 | continueBitOn).toByte :: remainingLengthEncoding(newX)
    }
  }

  override def encode: List[Byte] = remainingLengthEncoding(value)

  override def usedByte: Int = encode.length
}

case class STRING(value: String) extends DataFormat {
  val length: Short = if (value == null) 0 else value.getBytes.length.toShort


  override def encode: List[Byte] = {
    if (value == null) List()
    else INT(length).encode ++ value.getBytes.toList
  }

  override def usedByte: Int = encode.length
}

object EmptyDataFormat {
  val emptySTRING = STRING(null)
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
          if (x <= 0)
            List()
          else {
            val newX = x / 128
            def continueBitOn = if (newX > 0) 128 else 0
            (x % 128 | continueBitOn).toByte :: remainingLengthEncoding(newX)
          }
        }
        remainingLengthEncoding(remainingLength.value)
      }
    }
  }
}

object Decoder {

  case class ByteStreammer(bytes: Array[Byte]) {
    val pos = new AtomicInteger(0)
    val length = bytes.length

    def proceed(delta: DataFormat): Unit = pos.addAndGet(delta.usedByte)


    def rest =
      bytes.slice(pos.get(), length)

  }

  def decodeREMAININGLENGTH(streammer: ByteStreammer): REMAININGLENGTH = {
    val data = REMAININGLENGTH(remainingLengthDecoding(streammer.rest, 0, 1))
    streammer.proceed(data)
    data
  }

  def decodeBYTE(streammer: ByteStreammer): BYTE = {
    val data = BYTE(streammer.rest(0))
    streammer.proceed(data)
    data
  }

  def decodeINT(streammer: ByteStreammer): INT = {
    val data = INT(((streammer.rest(0) << 8 & 0xFF00) + streammer.rest(1)).toShort)
    streammer.proceed(data)
    data
  }


  def decodeSTRING(streammer: ByteStreammer): STRING = {
    val data = STRING(new String(streammer.rest.slice(2, 2 + (streammer.rest(0) << 8 & 0xFF00 | streammer.rest(1)).toShort)))
    streammer.proceed(data)
    data
  }

  private def remainingLengthDecoding(bytes: Array[Byte], index: Int, multiplier: Int): Int = {
    val encodeByte = bytes(index)
    val newValue = (encodeByte & 127) * multiplier
    if ((encodeByte & 128) == 0)
      newValue
    else
      newValue + remainingLengthDecoding(bytes, index + 1, multiplier * 128)
  }
}