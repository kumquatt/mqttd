package plantae.citrus.mqtt.dto

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

case class STRING(string: String) extends DataFormat {
  val length: Short = if (string == null) 0 else string.length.toShort


  override def encode: List[Byte] = {
    if (string == null) List()
    else INT(length).encode ++ string.getBytes.toList
  }

  def this(bytes: Array[Byte]) {
    this(new String(bytes.slice(2, 2 +(bytes(0) << 8 & 0xFF00| bytes(1)))))
  }

  override def usedByte: Int = encode.length
}


object Decoder {

  def decodeREMAININGLENGTH(bytes: Array[Byte]): REMAININGLENGTH = REMAININGLENGTH(remainingLengthDecoding(bytes, 0, 1))

  def decodeBYTE(bytes: Array[Byte]): BYTE = BYTE(bytes(0))

  def decodeINT(bytes: Array[Byte]): INT = INT(((bytes(0) << 8 & 0xFF00) + bytes(1)).toShort)

  def decodeSTRING(bytes: Array[Byte]): STRING = STRING(new String(bytes.slice(2, 2 + (bytes(0) << 8 | bytes(1)))))

  private def remainingLengthDecoding(bytes: Array[Byte], index: Int, multiplier: Int): Int = {
    val encodeByte = bytes(index)
    val newValue = (encodeByte & 127) * multiplier
    if ((encodeByte & 128) == 0)
      newValue
    else
      newValue + remainingLengthDecoding(bytes, index + 1, multiplier * 128)
  }

}