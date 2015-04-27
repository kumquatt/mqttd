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

          override def deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken): Unit = {

//            if (iMqttDeliveryToken.getMessage != null)
//              println(new String(iMqttDeliveryToken.getMessage.getPayload));
//            else
//              println(iMqttDeliveryToken);

          }

          override def messageArrived(s: String, mqttMessage: MqttMessage): Unit = {
            count = count + 1
            println("[ 1:" + count + " ]\tmessage:" + new String(mqttMessage.getPayload))
          }

          override def connectionLost(throwable: Throwable): Unit = {}
        }
      )
      option.setKeepAliveInterval(10)
      option.setWill("test", "test will message".getBytes, 2, true)
      client1.connect(option)
      println("client1 1 => connection complete")
      client1.subscribe(Array("test"))

      Range(1, 500).foreach(count => {
        println("publish " + count)
        client1.publish("test", ("qos 0 message" + count + " test publish").getBytes(), 1, true)
        //        Thread.sleep(100)
      }
      )
    }
  }.start()
  new Thread() {
    override def run: Unit = {
      var option = new MqttConnectOptions()
      var client1 = new MqttClient("tcp://localhost:8888", "customer_2")
      client1.setCallback(
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
      option.setKeepAliveInterval(10)
      option.setWill("test", "test will message".getBytes, 2, true)
      client1.connect(option)
      println("client2 => connection complete")

      client1.subscribe(Array("test"))

    }
  }

  while (true) {
    Thread.sleep(1000)
  }
}

