package plantae.citrus.mqtt.actors

import akka.actor.Actor
import akka.io.Tcp.{PeerClosed, Received, Write}
import akka.util.ByteString
import plantae.citrus.mqtt.dto.PacketDecoder
import plantae.citrus.mqtt.dto.connect.{CONNACK, CONNECT, DISCONNECT, ReturnCode}
import plantae.citrus.mqtt.dto.ping.{PINGREQ, PINGRESP}
import plantae.citrus.mqtt.dto.publish.PUBLISH
import plantae.citrus.mqtt.dto.subscribe.SUBSCRIBE
import plantae.citrus.mqtt.dto.unsubscribe.UNSUBSCRIBE

/**
 * Created by before30 on 15. 4. 21..
 */
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
