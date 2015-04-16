package plantae.citrus.exercise

import akka.actor.Actor
import akka.actor.Actor.Receive
import org.eclipse.paho.client.mqttv3.MqttClient

case object CONNECT_MQTT

class PahoClient extends Actor{

  def receive = {
    case CONNECT_MQTT =>
      println("Try to connect MQTT")
      val client = new MqttClient("tcp://localhost:8888", "pahomqttpublish1")
      client.connect()

  }
}
