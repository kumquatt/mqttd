package plantae.citrus.exercise

import org.eclipse.paho.client.mqttv3._


object Paho extends App {
  new Thread() {
    override def run: Unit = {
      var option = new MqttConnectOptions()
      var client1 = new MqttClient("tcp://127.0.0.1:1883", "customer_1")
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
      option.setWill("test", "test will message".getBytes, 2, true)
      client1.connect(option)
      println("client1 1 => connection complete")


      var client2 = new MqttClient("tcp://127.0.0.1:1883", "customer_2")
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

//      client1.subscribe("test")
      println("client 1 => subscribe test complete")

      client2.subscribe("test")

      client2.subscribe(Array("1", "1/1", "1/2", "1/3", "1/1/1", "1/1/1"), Array(1, 0, 0, 1, 2, 2))
      client2.subscribe("1", 2)

//      client1.subscribe("test")
//      println("client 2 => subscribe test complete")
//
//
//      client1.subscribe("a/1/c")
//      client1.subscribe("a/2/c")
//      client1.subscribe("a/3/c")
//      println("client1 a/1/c, a/2/c, a/3/c subscribed")
//
//      client1.publish("a/1/c", "a/1/c message, only client 1 will receive".getBytes, 0, false)
//      println("a/1/c message published")
//
//      client2.subscribe("a/+/c")
//      println("client2 a/+/c subscribed")
//
//      client1.publish("a/1/c", "a/1/c message, both client1 and 2 will receive".getBytes, 0, false)
//      println("a/1/c message published")
//      client1.publish("a/2/c", "a/2/c message, both client1 and 2 will receive".getBytes, 0, false)
//      println("a/2/c message published")
//      client1.publish("a/3/c", "a/3/c message, both client1 and 2 will receive".getBytes, 0, false)
//      println("a/3/c message published")





//      client1.publish("test", "qos 0 message".getBytes, 0, false)
//      println("publish complete qos 0")
//
//      client1.publish("test", "qos 1 message".getBytes, 1, false)
//      println("publish complete qos 1")
//
//      client1.publish("test", "qos 2 message".getBytes, 2, false)
//      println("publish complete qos 2")

//      println("sleep 10 seconds")
//
//      Range(1, 100000).foreach(x => {
//
//        client1.publish("test", "qos 2 count(%d) th ... message".format(x.toInt).getBytes, 2, false)
//        client1.publish("test", "qos 0 count(%d) th ... message".format(x.toInt).getBytes, 0, false)
//        client1.publish("test", "qos 1 count(%d) th ... message".format(x.toInt).getBytes, 2, false)
//        println("test count("+x+ ") message sned")
////        Thread.sleep(10)
//      })

      Range(1, 100).foreach(x => {
        Thread.sleep(1000)
        println(x + " second passed")
      })

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

