package plantae.citrus

import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import plantae.citrus.exercise.{DISCONNECT_MQTT, CONNECT_MQTT, PahoClient, StartUpMessage}


object Kumquatt {

  def main(args: Array[String]): Unit = {


//    val system = ActorSystem("kumquatt", ConfigFactory.load.getConfig("mqtt-broker"))
//    val server = system.actorOf(Props[KumquattServer], name = "kumquatt")
//    server ! StartUpMessage
//
//    val paho = system.actorOf(Props[PahoClient], name = "mqttClient")
//    paho ! CONNECT_MQTT
//    Thread.sleep(10000)
//    paho ! DISCONNECT_MQTT

  }
}


