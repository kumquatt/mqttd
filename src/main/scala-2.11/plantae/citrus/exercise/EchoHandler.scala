package plantae.citrus.exercise

import akka.actor.Actor
import akka.io.Tcp.{PeerClosed, Write, Received}
import akka.util.ByteString
import plantae.citrus.mqtt.dto.{PacketDecoder, Decoder}
import plantae.citrus.mqtt.dto.connect._
import plantae.citrus.mqtt.dto.ping.{PINGRESP, PINGREQ}
import plantae.citrus.mqtt.dto.publish.{PUBACK, PUBLISH}
import plantae.citrus.mqtt.dto.subscribe.SUBSCRIBE
import plantae.citrus.mqtt.dto.unsubscribe.UNSUBSCRIBE

class EchoHandler extends Actor{
  var idx = 0

  def receive = {
    // 소켓쪽으로 무언가 들어왔을 경우

    case Received(data) =>

      println(data.toArray[Byte])

      PacketDecoder.decode(data.toArray[Byte]) match {
        case CONNECT(_, _, _, _, _) =>
          printf("CONNECT")
          val returnString = CONNACK(true, ReturnCode.connectionAccepted).encode
          sender() ! Write(ByteString(returnString))
        case PINGREQ =>
          printf("PING")
          sender() ! Write(ByteString(PINGRESP.encode))
        case p @ PUBLISH(_,_,_,_,_,_) =>
          // TODO :
          // actor ! PUBLISH(p)
//          sender() ! Write(ByteString(PUBACK.))
        case DISCONNECT =>
          context.stop(self)
//        case SUBSCRIBE =>
//        case UNSUBSCRIBE =>

      }
      // ByteString으로 들어온다.
    case PeerClosed => context.stop(self)
//    case // from  ClientActor
  }
}
