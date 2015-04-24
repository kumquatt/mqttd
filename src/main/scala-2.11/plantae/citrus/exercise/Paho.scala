package plantae.citrus.exercise

import org.eclipse.paho.client.mqttv3._


object Paho extends App {
  new Thread() {
    override def run: Unit = {
      var option = new MqttConnectOptions()
      var client = new MqttClient("tcp://10.202.208.200:8888", "customer_1")
      client.setCallback(
        new MqttCallback {
          override def deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken): Unit = {}

          override def messageArrived(s: String, mqttMessage: MqttMessage): Unit = {
            println("topic:"+s+"\tmessage:"+new String(mqttMessage.getPayload))
          }

          override def connectionLost(throwable: Throwable): Unit = {}
        }
      )
      option.setKeepAliveInterval(10)
      client.connect(option)
      println("connection complete")
      client.subscribe("test")
      println("subscribe test complete")
      //
      //      client.subscribe("test1")
      //      println("subscribe test1 complete")

      //      client.publish("test", "qos 0 message".getBytes, 0, false)
      //      println("publish complete qos 0")

//      client.publish("test", "qos 1 message".getBytes, 1, false)
//      println("publish complete qos 1")
//
      client.publish("test", "qos 2 message".getBytes, 2, false)
      println("publish complete qos 2")
      //
//      client.publish("test", "qos 0 message".getBytes, 0, false)
//      println("publish complete qos 0")
      //
      //      client.publish("test", "qos 0 message".getBytes, 0, false)
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

