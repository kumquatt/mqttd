package plantae.citrus.exercise

import org.eclipse.paho.client.mqttv3.{MqttClient, MqttConnectOptions}


object Paho extends App {
  new Thread() {
    override def run: Unit = {
      var option = new MqttConnectOptions()
      var client = new MqttClient("tcp://localhost:8888", "customer_1")
      option.setCleanSession(true)
      client.connect(option)
      //      Thread.sleep(3000)
      //      client.publish("test topic", "test payload bytes".getBytes(), 2, true)

    }
  }.start()

  //    Thread.sleep(1000)
  //    new Thread() {
  //      override def run: Unit = {
  //        var client = new MqttClient("tcp://localhost:8888", "customer_2").connect()
  //
  //      }
  //    }.start()
  //
  //    Thread.sleep(1000)
  //    new Thread() {
  //      override def run: Unit = {
  //        var client = new MqttClient("tcp://localhost:8888", "customer_3").connect()
  //
  //      }
  //    }.start()
  //
  //    Thread.sleep(1000)
  //    new Thread() {
  //      override def run: Unit = {
  //        var client = new MqttClient("tcp://localhost:8888", "customer_4").connect()
  //
  //      }
  //    }.start()
  while (true) {
    Thread.sleep(1000)
  }
}

