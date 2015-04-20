package plantae.citrus.mqtt.dto.ping

import plantae.citrus.mqtt.dto._

case object PINGREQ extends Packet {
  override def fixedHeader: FixedHeader = FixedHeader(ControlPacketType.PINGREQ, REMAININGLENGTH(0))
  override def variableHeader: VariableHeader = EMPTY_COMPONENT.EMPTY_VARIABLE_HEADER
  override def payload: Payload = EMPTY_COMPONENT.EMPTY_PAYLOAD
  override def usedByte: Int = encode.length
}

object PINGREQDecoder {
  def decode(bytes: Array[Byte]): Packet = {
    PINGREQ
  }
}

case object PINGRESP extends Packet {
  override def fixedHeader: FixedHeader = FixedHeader(ControlPacketType.PINGRESP, REMAININGLENGTH(0))
  override def variableHeader: VariableHeader = EMPTY_COMPONENT.EMPTY_VARIABLE_HEADER
  override def payload: Payload = EMPTY_COMPONENT.EMPTY_PAYLOAD
  override def usedByte: Int = encode.length
}

object PINGRESPDecoder {
  def decode(bytes: Array[Byte]): Packet = {
    PINGRESP
  }
}