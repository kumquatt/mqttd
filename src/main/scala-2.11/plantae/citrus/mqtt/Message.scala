package plantae.citrus.mqtt

case class Message(topic:String,
                   message:String,
                   QoS:QosLevel.Value,
                   retain:Boolean)