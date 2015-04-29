package plantae.citrus.exercise

import org.eclipse.paho.client.mqttv3._


object PublishTest extends App {
//  val port = 1883
  val port = 8888
  var option = new MqttConnectOptions()
  var client1 = new MqttClient("tcp://localhost:" + port, "customer1")
  client1.setCallback(
    new MqttCallback {
      var count = 0

      override def deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken): Unit = {

      }

      override def messageArrived(s: String, mqttMessage: MqttMessage): Unit = {
        count = count + 1
        println("[ 1:" + count + " ]\tmessage:" + new String(mqttMessage.getPayload))
      }

      override def connectionLost(throwable: Throwable): Unit = {}
    }
  )
  option.setKeepAliveInterval(100)
  option.setCleanSession(true)
  client1.connect(option)
  println("client1 => connection complete")
  client1.subscribe(Array("test2"))
  println("client1 => subscribe")


  var option2 = new MqttConnectOptions()
  var client2 = new MqttClient("tcp://localhost:"+ port, "customer2")
  client2.setCallback(
    new MqttCallback {
      var count = 0

      override def deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken): Unit = {}

      override def messageArrived(s: String, mqttMessage: MqttMessage): Unit = {
        count = count + 1
        println("[ 2:" + count + " ]\tmessage:" + new String(mqttMessage.getPayload))
      }

      override def connectionLost(throwable: Throwable): Unit = {}
    }
  )
  option2.setKeepAliveInterval(100)
  option2.setCleanSession(true)
  client2.connect(option2)
  println("client2 => connection complete")

  client2.subscribe(Array("test2"))
  println("client2 => subscribe complete")

  Range(1, 10000).foreach(count => {
    println("publish " + count)
    client1.publish("test2", ("qos 0 message " + count + " test publish public static void main(String[] args)" ).getBytes(), 2, false)
  }
  )
  Thread.sleep(10000)
  client1.disconnect()
  println("client1 => disconnection complete")


  while (true) {
    Thread.sleep(1000)
  }
}

