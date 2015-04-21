package plantae.citrus.exercise

sealed trait  Message

case class StartUpMessage(name:String) extends Message
case class SomeMessage(msg: String) extends Message
