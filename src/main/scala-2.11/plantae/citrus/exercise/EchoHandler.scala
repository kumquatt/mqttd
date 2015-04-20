package plantae.citrus.exercise

import akka.actor.Actor
import akka.io.Tcp.{PeerClosed, Write, Received}
import akka.util.ByteString
import plantae.citrus.mqtt.dto.connect.{ReturnCode, CONNACK, CONNECTDecoder}

class EchoHandler extends Actor{

  def receive = {
    case Received(data) =>
      println(data)

      val connect = CONNECTDecoder.decode(data.toArray[Byte])
      val returnString = CONNACK(true, ReturnCode.connectionAccepted).encode
      sender() ! Write(ByteString(returnString))

      // ByteString으로 들어온다.
    case PeerClosed => context.stop(self)
  }
}
