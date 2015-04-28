package plantae.citrus.exercise

import org.eclipse.paho.client.mqttv3._


object PublishTest extends App {
  var option = new MqttConnectOptions()
  var client1 = new MqttClient("tcp://localhost:8888", "customer_1")
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
  client1.connect(option)
  println("client1 => connection complete")
  client1.subscribe(Array("test"))
  Thread.sleep(1000)


  var option2 = new MqttConnectOptions()
  var client2 = new MqttClient("tcp://localhost:8888", "customer_2")
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
  client2.connect(option2)
  println("client2 => connection complete")

  client2.subscribe(Array("test2"))
  println("client2 => subscribe complete")

  Range(1, 5000).foreach(count => {
    println("publish " + count)
    client1.publish("test2", ("qos 0 message " + count + " test publish").getBytes(), 0, false)
  }
  )
  client1.disconnect()
  println("client1 => disconnection complete")



  while (true) {
    Thread.sleep(1000)
  }
}

