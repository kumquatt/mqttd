package plantae.citrus.exercise

import org.eclipse.paho.client.mqttv3._


object Paho extends App {
  new Thread() {
    override def run: Unit = {
      var option = new MqttConnectOptions()
      var client1 = new MqttClient("tcp://localhost:8888", "customer_1")
      client1.setCallback(
        new MqttCallback {
          override def deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken): Unit = {}

          override def messageArrived(s: String, mqttMessage: MqttMessage): Unit = {
            println("client1 1 => topic:" + s + "\tmessage:" + new String(mqttMessage.getPayload))
          }

          override def connectionLost(throwable: Throwable): Unit = {}
        }
      )
      option.setKeepAliveInterval(10)
      option.setWill("test","test will message".getBytes,2, true)
      client1.connect(option)
      println("client1 1 => connection complete")


      var client2 = new MqttClient("tcp://localhost:8888", "customer_2")
      client2.setCallback(
        new MqttCallback {
          override def deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken): Unit = {}

          override def messageArrived(s: String, mqttMessage: MqttMessage): Unit = {
            println("client1 2 => topic:" + s + "\tmessage:" + new String(mqttMessage.getPayload))
          }

          override def connectionLost(throwable: Throwable): Unit = {}
        }
      )
      option.setKeepAliveInterval(10)

      client2.connect(option)

      client1.subscribe("test")
      println("client 1 => subscribe test complete")

      client2.subscribe("test")
      println("client 2 => subscribe test complete")
      client2.disconnect()
      println("client 2 => disconnect")

      //
      //      client1.subscribe("test1")
      //      println("subscribe test1 complete")





      client1.publish("test", "qos 0 message".getBytes, 0, false)
      println("publish complete qos 0")

      client1.publish("test", "qos 1 message".getBytes, 1, false)
      println("publish complete qos 1")

      client1.publish("test", "qos 2 message".getBytes, 2, false)
      println("publish complete qos 2")

      println("sleep 10 seconds")
      Range(1, 10).foreach(x => {
        Thread.sleep(1000)
        println(x + " second passed")
      })

      client2 = new MqttClient("tcp://localhost:8888", "customer_2")
      client2.setCallback(
        new MqttCallback {
          override def deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken): Unit = {}

          override def messageArrived(s: String, mqttMessage: MqttMessage): Unit = {
            println("client2 topic:" + s + "\tmessage:" + new String(mqttMessage.getPayload))
          }

          override def connectionLost(throwable: Throwable): Unit = {}
        }
      )
      option.setKeepAliveInterval(10)
      option.setCleanSession(false)
      client2.connect(option)

      println("client2 connection complete")

      //
      //      client1.publish("test", "qos 2 message".getBytes, 2, false)
      //      println("publish complete qos 2")
      //
      //      client1.publish("test", "qos 0 message".getBytes, 0, false)
      //      println("publish complete qos 0")
      //
      //      client1.publish("test", "qos 0 message".getBytes, 0, false)
      //      println("publish complete qos 0")

    }
  }.start()

  //  Thread.sleep(1000)
  //  new Thread() {
  //    override def run: Unit = {
  //      var client = new MqttClient("tcp://localhost:8888", "customer_2")
  //
  //      client.connect()
  //      client.subscribe("test")
  //      client.subscribe("test1")
  //
  //    }
  //  }.start()
  //
  //  Thread.sleep(1000)
  //  new Thread() {
  //    override def run: Unit = {
  //      var client = new MqttClient("tcp://localhost:8888", "customer_3")
  //
  //      client.connect()
  //      client.subscribe("test")
  //      client.subscribe("test1")
  //
  //    }
  //  }.start()
  //
  //  Thread.sleep(1000)
  //  new Thread() {
  //    override def run: Unit = {
  //      var client = new MqttClient("tcp://localhost:8888", "customer_4")
  //
  //
  //      client.connect()
  //      client.subscribe("test")
  //      client.subscribe("test1")
  //
  //    }
  //  }.start()
  while (true) {
    Thread.sleep(1000)
  }
}

