package plantae.citrus.mqtt

case class Header (
  controlPacketType: ControlPacketType.Value,
  dup: Boolean,
  qos: QosLevel.Value,
  retain: Boolean,
  remainingLength: Long
)

