package plantae.citrus

import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import plantae.citrus.exercise.{DISCONNECT_MQTT, CONNECT_MQTT, PahoClient, StartUpMessage}
import plantae.citrus.mqtt.actors.SystemRoot
import plantae.citrus.mqtt.actors.connection.Server

object Launcher extends App {
  val actor = SystemRoot.system.actorOf(Props[Server], name = "broker")
}
