package plantae.citrus.exercise

import org.eclipse.paho.client.mqttv3._


object PublishTest extends App {
  val port = 8888
  //  val host = "10.202.32.42"
  val host = "127.0.0.1"
  val target = "tcp://" + host + ":" + port
  var option1 = new MqttConnectOptions()
  var client1 = new MqttClient(target, "customer1")
  option1.setKeepAliveInterval(100)
  option1.setCleanSession(true)
  client1.setCallback(
    new MqttCallback {
      var count = 0

      override def deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken): Unit = {}

      override def messageArrived(s: String, mqttMessage: MqttMessage): Unit = {
        count = count + 1
        if (count % 100 == 0)
          println("[ 1:" + count + " ]\tqos : " + mqttMessage.getQos + "\tmessage:" + new String(mqttMessage.getPayload))
      }

      override def connectionLost(throwable: Throwable): Unit = {}
    }
  )

  client1.connect(option1)
  println("client1 => connection complete")
  client1.subscribe(("test2"), 1)
  println("client1 => subscribe")
  client1.subscribe(("test2"), 1)
  println("client1 => subscribe")


  var option2 = new MqttConnectOptions()
  var client2 = new MqttClient(target, "customer2")
  client2.setCallback(
    new MqttCallback {
      var count = 0

      override def deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken): Unit = {}

      override def messageArrived(s: String, mqttMessage: MqttMessage): Unit = {
        count = count + 1
        if (count % 100 == 0)
          println("[ 2:" + count + " ]\tmessage:" + new String(mqttMessage.getPayload))
      }

      override def connectionLost(throwable: Throwable): Unit = {}
    }
  )
  option2.setKeepAliveInterval(100)
  option2.setCleanSession(false)
  client2.connect(option2)
  println("client2 => connection complete")

  client2.subscribe(("test2"), 1)
  println("client2 => subscribe complete")


  //  var option3 = new MqttConnectOptions()
  //  var client3 = new MqttClient(target, "customer3")
  //  client3.setCallback(
  //    new MqttCallback {
  //      var count = 0
  //
  //      override def deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken): Unit = {}
  //
  //      override def messageArrived(s: String, mqttMessage: MqttMessage): Unit = {
  //        count = count + 1
  //        if (count % 100 == 0)
  //          println("[ 3:" + count + " ]\tmessage:" + new String(mqttMessage.getPayload))
  //      }
  //
  //      override def connectionLost(throwable: Throwable): Unit = {}
  //    }
  //  )
  //  option3.setKeepAliveInterval(100)
  //  option3.setCleanSession(false)
  //  client3.connect(option3)
  //  println("client3 => connection complete")
  //
  //  client3.subscribe(("test2"), 1)
  //  Thread.sleep(1000)
  //  println("client3 => subscribe complete")
  //  client3.disconnect()
  //  println("client3 => disconnect")

  Thread.sleep(1000)

  Range(1, 100000).foreach(count => {
    if (count % 100 == 0)
      println("publish " + count)
    client1.publish("test2", ("qos 0 message " + count + " test publish public static void main(String[] args)").getBytes(), 0, false)
  }
  )
  //  client3.setCallback(
  //    new MqttCallback {
  //      var count = 0
  //
  //      override def deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken): Unit = {}
  //
  //      override def messageArrived(s: String, mqttMessage: MqttMessage): Unit = {
  //        count = count + 1
  //        println("[ 3:" + count + " ]\tmessage:" + new String(mqttMessage.getPayload))
  //      }
  //
  //      override def connectionLost(throwable: Throwable): Unit = {}
  //    }
  //  )
  //  option3.setCleanSession(false)
  //  client3.connect(option3)
  //  println("client3 => connection complete")

  Thread.sleep(100000)
  client1.disconnect()
  println("client1 => disconnection complete")


  while (true) {
    Thread.sleep(1000)
  }
}

object Test3 extends App {
  val port = 1883
  //    val host = "10.202.32.42"
  val host = "10.202.32.45"
  val target = "tcp://" + host + ":" + port
  var option3 = new MqttConnectOptions()
  var client3 = new MqttClient(target, "customer3")
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
  client3.subscribe(("test2"), 1)
  client3.publish("a/b" ,"topic test12".getBytes,1, false)
  println("topic publish complete")
  //  Thread.sleep(1000)
  Thread.sleep(100000)
  client3.disconnect()
  while (true) {
    Thread.sleep(1000)
  }


}


object TopicPublish extends App {
  val port = 8888
  //    val host = "10.202.32.42"
  val host = "127.0.0.1"
  val target = "tcp://" + host + ":" + port
  Range(1, 500).foreach(each => {
    val option3 = new MqttConnectOptions()
    val client3 = new MqttClient(target, "customer" + each)
    client3.setCallback(
      new MqttCallback {
        var count = 0

        override def deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken): Unit = {}

        override def messageArrived(s: String, mqttMessage: MqttMessage): Unit = {
          count = count + 1
          println("[ " + each + ":" + count + " ]\tmessage:" + new String(mqttMessage.getPayload))
        }

        override def connectionLost(throwable: Throwable): Unit = {}
      }
    )
    option3.setKeepAliveInterval(100)
    option3.setCleanSession(true)
    client3.connect(option3)
    println("client3 => connection complete")
    client3.subscribe(("a/b"), 1)
  }
  )


  var option3 = new MqttConnectOptions()
  var client3 = new MqttClient(target, "customer" + "publisher")
  client3.connect()
  client3.publish("a/b", "topic test message".getBytes, 2, false)

  //  Thread.sleep(1000)
  Thread.sleep(100000)
  while (true) {
    Thread.sleep(1000)
  }


}

