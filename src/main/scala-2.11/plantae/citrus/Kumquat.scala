package plantae.citrus

import java.net.InetSocketAddress

import akka.actor.{Props, Actor, ActorSystem}
import akka.io.Tcp.{Close, Write, PeerClosed, Received}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import plantae.citrus.exercise.{DISCONNECT_MQTT, CONNECT_MQTT, PahoClient, StartUpMessage}
import plantae.citrus.mqtt.dto.PacketDecoder
import plantae.citrus.mqtt.dto.connect.{DISCONNECT, ReturnCode, CONNACK, CONNECT}
import plantae.citrus.mqtt.dto.ping.{PINGRESP, PINGREQ}
import plantae.citrus.mqtt.dto.publish.PUBLISH
import plantae.citrus.mqtt.dto.subscribe.SUBSCRIBE
import plantae.citrus.mqtt.dto.unsubscribe.UNSUBSCRIBE

object Kumquat {

  def main(args: Array[String]): Unit = {


    val system = ActorSystem("kumquatt", ConfigFactory.load.getConfig("mqtt-broker"))
    val server = system.actorOf(Props[KumquatServer], name = "kumquatt")
    server ! StartUpMessage

    val paho = system.actorOf(Props[PahoClient], name = "mqttClient")
    paho ! CONNECT_MQTT
    Thread.sleep(10000)
    paho ! DISCONNECT_MQTT

  }
}

class KumquatServer extends Actor {
  import context.system
  import Tcp._

  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 8888))

  def receive = {
    case StartUpMessage =>
      println("kuMQuaTT broker start")

    case b @ Bound(localAddress) =>

    case CommandFailed(_: Bind) => context.stop(self)

    case c @ Connected(remote, local) =>
      val handler = context.actorOf(Props[MqttHandler])
      val connection = sender()
      connection ! Register(handler)

  }
}

class MqttHandler extends Actor {

  def receive = {
    case Received(data) => {
      println(data)

      PacketDecoder.decode(data.toArray[Byte]) match {
        case c @ CONNECT(_, _, _, _, _) =>
          sender() ! Write(ByteString(CONNACK(true, ReturnCode.connectionAccepted).encode))

        case PINGREQ =>
          sender() ! Write(ByteString(PINGRESP.encode))

        case p @ PUBLISH(_, _, _, _, _, _) =>

        case DISCONNECT =>
          self ! Close

        case s @ SUBSCRIBE(_, _) =>

        case u @ UNSUBSCRIBE(_, _) =>
      }
    }

    case PeerClosed =>
      context.stop(self)
  }
}
