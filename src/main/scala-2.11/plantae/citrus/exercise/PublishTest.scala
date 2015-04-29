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
  var client2 = new MqttClient("tcp://localhost:" + port, "customer2")
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
  option2.setCleanSession(false)
  client2.connect(option2)
  println("client2 => connection complete")

  client2.subscribe(Array("test2"))
  println("client2 => subscribe complete")


  var option3 = new MqttConnectOptions()
  var client3 = new MqttClient("tcp://localhost:" + port, "customer3")
  client3.setCallback(
    new MqttCallback {
      var count = 0

      override def deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken): Unit = {}

      override def messageArrived(s: String, mqttMessage: MqttMessage): Unit = {
        count = count + 1
        println("[ 3:" + count + " ]\tmessage:" + new String(mqttMessage.getPayload))
      }

      override def connectionLost(throwable: Throwable): Unit = {}
    }
  )
  option3.setKeepAliveInterval(100)
  option3.setCleanSession(false)
  client3.connect(option3)
  println("client3 => connection complete")

  client3.subscribe(Array("test2"))
  println("client3 => subscribe complete")
  client3.disconnect()


  Range(1, 3000).foreach(count => {
    println("publish " + count)
    client1.publish("test2", ("qos 0 message " + count + " test publish public static void main(String[] args)").getBytes(), 0, false)
  }
  )
  client3.setCallback(
    new MqttCallback {
      var count = 0

      override def deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken): Unit = {}

      override def messageArrived(s: String, mqttMessage: MqttMessage): Unit = {
        count = count + 1
        println("[ 3:" + count + " ]\tmessage:" + new String(mqttMessage.getPayload))
      }

      override def connectionLost(throwable: Throwable): Unit = {}
    }
  )
  option3.setCleanSession(true)
  client3.connect(option3)
  println("client3 => connection complete")

  Thread.sleep(10000)
  client1.disconnect()
  println("client1 => disconnection complete")


  while (true) {
    Thread.sleep(1000)
  }
}

