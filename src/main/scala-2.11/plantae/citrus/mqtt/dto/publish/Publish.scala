package plantae.citrus.mqtt.dto.publish

import plantae.citrus.mqtt.dto.Decoder.ByteStream
import plantae.citrus.mqtt.dto._

sealed trait MESSAGE extends Packet

case class PUBLISH(dup: Boolean, qos: INT, retain: Boolean, topic: STRING, packetId: Option[INT], data: PUBLISHPAYLOAD) extends MESSAGE {

  override def variableHeader: VariableHeader = VariableHeader(packetId match {
    case Some(x) => List(topic, x)
    case None => List(topic)
  })

  override def payload: Payload = Payload(List(data))

  override def fixedHeader: FixedHeader = {
    FixedHeader({
      BYTE(0x03) << 4 | {
        if (dup) BYTE(0x01) << 3
        else BYTE(0x00)
      } | {
        if (qos > 3 || qos < 0)
          throw new Error()
        else
          qos.toBYTE << 1
      } | {
        if (retain) BYTE(0x01)
        else BYTE(0x00)
      }
    }, REMAININGLENGTH(variableHeader.usedByte + payload.usedByte))
  }

  override def usedByte: Int = encode.length
}


case class PUBACK(packetId: INT) extends MESSAGE {

  override def variableHeader: VariableHeader = VariableHeader(List(packetId))

  override def payload: Payload = EMPTY_COMPONENT.EMPTY_PAYLOAD

  override def fixedHeader: FixedHeader = FixedHeader(BYTE(0x40), REMAININGLENGTH(variableHeader.usedByte + payload.usedByte))

  override def usedByte: Int = encode.length
}


case class PUBREC(packetId: INT) extends MESSAGE {

  override def variableHeader: VariableHeader = VariableHeader(List(packetId))

  override def payload: Payload = EMPTY_COMPONENT.EMPTY_PAYLOAD

  override def fixedHeader: FixedHeader = FixedHeader(BYTE(0x50), REMAININGLENGTH(variableHeader.usedByte + payload.usedByte))

  override def usedByte: Int = encode.length
}

case class PUBREL(packetId: INT) extends MESSAGE {

  override def variableHeader: VariableHeader = VariableHeader(List(packetId))

  override def payload: Payload = EMPTY_COMPONENT.EMPTY_PAYLOAD

  override def fixedHeader: FixedHeader = FixedHeader(BYTE(0x62), REMAININGLENGTH(variableHeader.usedByte + payload.usedByte))

  override def usedByte: Int = encode.length
}

case class PUBCOMB(packetId: INT) extends MESSAGE {

  override def variableHeader: VariableHeader = VariableHeader(List(packetId))

  override def payload: Payload = EMPTY_COMPONENT.EMPTY_PAYLOAD

  override def fixedHeader: FixedHeader = FixedHeader(BYTE(0x70), REMAININGLENGTH(variableHeader.usedByte + payload.usedByte))

  override def usedByte: Int = encode.length
}

object PUBLISHDecoder {
  val dupBit = BYTE(0x01) << 3
  val qosBit = BYTE(0x03) << 1
  val retainBit = BYTE(0x01)
  val typeBit = BYTE(0xF0.toByte)

  def decode(bytes: Array[Byte]): PUBLISH = {
    val stream = ByteStream(bytes)
    val typeAndFlag = Decoder.decodeBYTE(stream)
    if ((typeAndFlag & typeBit) != BYTE(0x30.toByte))
      throw new Error
    val remainingLength = Decoder.decodeREMAININGLENGTH(stream)
    val topic = Decoder.decodeSTRING(stream)
    val packetId = {
      if (((typeAndFlag & qosBit) >> 1).toINT > 0)
        Some(Decoder.decodeINT(stream))
      else None
    }
    val payload = Decoder.decodePUBLISHPAYLOAD(stream, remainingLength.value - topic.usedByte - {
      packetId match {
        case Some(x) => x.usedByte
        case None => 0
      }
    })

    val publish = PUBLISH((typeAndFlag & dupBit).toBoolean, ((typeAndFlag & qosBit) >> 1).toINT, (typeAndFlag & retainBit).toBoolean, topic, packetId, payload)
    if (remainingLength.value != publish.variableHeader.usedByte + publish.payload.usedByte) {
      println("header said " + remainingLength.value + " but real size is " +(publish.variableHeader.usedByte + publish.payload.usedByte))
      throw new Error
    }
    publish
  }
}


object PUBACKDecoder {
  def decode(bytes: Array[Byte]): PUBACK = {
    val stream = ByteStream(bytes)
    val typeAndFlag = Decoder.decodeBYTE(stream)
    if (typeAndFlag != BYTE(0x40)) throw new Error
    val remainingLength = Decoder.decodeREMAININGLENGTH(stream)
    val packetId = Decoder.decodeINT(stream)
    PUBACK(packetId)
  }

}

object PUBRECDecoder {
  def decode(bytes: Array[Byte]): PUBREC = {
    val stream = ByteStream(bytes)
    val typeAndFlag = Decoder.decodeBYTE(stream)
    if (typeAndFlag != BYTE(0x50)) throw new Error
    val remainingLength = Decoder.decodeREMAININGLENGTH(stream)
    val packetId = Decoder.decodeINT(stream)
    PUBREC(packetId)
  }

}

object PUBRELDecoder {
  def decode(bytes: Array[Byte]): PUBREL = {
    val stream = ByteStream(bytes)
    val typeAndFlag = Decoder.decodeBYTE(stream)
    val remainingLength = Decoder.decodeREMAININGLENGTH(stream)
    if (typeAndFlag != BYTE(0x62.toByte)) {
      println("expected is " + BYTE(0x62).hexa + " but real is " + (typeAndFlag).hexa)
      throw new Error
    }
    val packetId = Decoder.decodeINT(stream)
    PUBREL(packetId)
  }

}

object PUBCOMBDecoder {
  def decode(bytes: Array[Byte]): PUBCOMB = {
    val stream = ByteStream(bytes)
    val typeAndFlag = Decoder.decodeBYTE(stream)
    val remainingLength = Decoder.decodeREMAININGLENGTH(stream)
    if (typeAndFlag != BYTE(0x70)) {
      println("expected is " + BYTE(0x70).hexa + " but real is " + (typeAndFlag).hexa + " : " + bytes.length + " " + new String(bytes))
      throw new Error
    }
    val packetId = Decoder.decodeINT(stream)
    PUBCOMB(packetId)
  }

}