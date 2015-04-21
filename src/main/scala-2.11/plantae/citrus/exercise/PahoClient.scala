package plantae.citrus.exercise

import akka.actor.Actor
import akka.actor.Actor.Receive
import org.eclipse.paho.client.mqttv3.{MqttConnectOptions, MqttMessage, MqttClient}

case object CONNECT_MQTT
case object DISCONNECT_MQTT
case object SUBSCRIBE_MQTT
case object UNSUBSCRIBE_MQTT
case object PUBLISH_MQTT

class PahoClient extends Actor{

  val client = new MqttClient("tcp://localhost:8888", "client_id")

  client.connect()
  def receive = {
    case CONNECT_MQTT =>
      println("Try to connect MQTT")

      client.connect()
      println("mqtt connected? " + client.isConnected)

    case DISCONNECT_MQTT =>
      println("Try to disconnect MQTT")
      client.disconnect()

    case SUBSCRIBE_MQTT =>
      println("Try to subscribe MQTT")
      client.subscribe("topic1")

    case UNSUBSCRIBE_MQTT =>
      println("Try to unsubscribe MQTT")
      client.unsubscribe("topic1")

    case PUBLISH_MQTT =>
      println("Try to publish MQTT")
      val mqttMessage = new MqttMessage()
      mqttMessage.setQos(1)
      mqttMessage.setRetained(false)
//      mqttMessage.setPayload([0,0]
      client.publish("topic1", mqttMessage)

    case _ =>
      println("....")


  }
}
