package plantae.citrus.exercise

import org.eclipse.paho.client.mqttv3.{MqttClient, MqttConnectOptions}


object Paho extends App {
  new Thread() {
    override def run: Unit = {
      var option = new MqttConnectOptions()
      var client = new MqttClient("tcp://localhost:8888", "customer_1")
      option.setKeepAliveInterval(10)
      client.connect(option)
      println("connection complete")
      client.subscribe("test")
      println("subscribe test complete")

      client.subscribe("test1")
      println("subscribe test1 complete")

      client.publish("test", "test message".getBytes, 1, false)
      println("publish complete")
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

