package plantae.citrus.mqtt

//case class Header (
//  fixedHeader: FixedHeader,
//  variableHeader: VariableHeader
//)

case class FixedHeader (
  controlPacketType: ControlPacketType.Value,
  dup: Boolean,
  qos: QosLevel.Value,
  retain: Boolean,
  remainingLength: Long
)

//trait VariableHeader