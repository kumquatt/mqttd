package plantae.citrus.exercise

sealed trait  Message

case object StartUpMessage extends Message
case class SomeMessage(msg: String) extends Message
