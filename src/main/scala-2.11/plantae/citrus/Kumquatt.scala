package plantae.citrus

import akka.actor.Props
import plantae.citrus.mqtt.actors.SystemRoot
import plantae.citrus.mqtt.actors.connection.Server

object Launcher extends App {
  val actor = SystemRoot.system.actorOf(Props[Server], name = "broker")
}
