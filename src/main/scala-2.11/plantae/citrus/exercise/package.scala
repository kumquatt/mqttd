package plantae.citrus

import org.eclipse.paho.client.mqttv3.{MqttClient, MqttConnectOptions}

/**
 * Created by yinjae on 15. 4. 28..
 */
package object exercise extends App {
  var option = new MqttConnectOptions()
  var client1 = new MqttClient("tcp://localhost:8888", "customer2")
  client1.connect()


  client1.publish("topic3", "qos test for topic test3".getBytes, 1, true)


  client1.close()
}
