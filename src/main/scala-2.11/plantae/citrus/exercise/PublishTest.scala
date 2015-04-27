package plantae.citrus.exercise

import org.eclipse.paho.client.mqttv3._


object PublishTest extends App {
  new Thread() {
    override def run: Unit = {
      var option = new MqttConnectOptions()
      var client1 = new MqttClient("tcp://localhost:8888", "customer_1")
      client1.setCallback(
        new MqttCallback {
          var count = 0

          override def deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken): Unit = {}

          override def messageArrived(s: String, mqttMessage: MqttMessage): Unit = {
            count = count + 1
            println("[ " + count + " ]\tmessage:" + new String(mqttMessage.getPayload))
          }

          override def connectionLost(throwable: Throwable): Unit = {}
        }
      )
      option.setKeepAliveInterval(10)
      option.setWill("test", "test will message".getBytes, 2, true)
      client1.connect(option)
      client1.subscribe(Array("test"))

      Range(1, 1000).foreach(count =>
        client1.publish("test", (count + "test publish").getBytes(), 1, true)
      )
      println("client1 1 => connection complete")
    }
  }.start()

  while (true) {
    Thread.sleep(1000)
  }
}

