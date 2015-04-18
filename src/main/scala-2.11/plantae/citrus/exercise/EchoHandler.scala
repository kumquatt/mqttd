package plantae.citrus.exercise

import akka.actor.Actor
import akka.io.Tcp.{PeerClosed, Write, Received}
import plantae.citrus.mqtt.domains.CONNECT

class EchoHandler extends Actor{

  def receive = {
    case Received(data) =>
      println(data)
      val connect = CONNECT.decode(data)
      println(connect.header, connect.keepAliveInSeconds, connect.password, connect.protocolName, connect.protocolVersion, connect.username, connect.willMessage)
      sender() ! Write(data)
      // ByteString으로 들어온다.
    case PeerClosed => context.stop(self)
  }
}
