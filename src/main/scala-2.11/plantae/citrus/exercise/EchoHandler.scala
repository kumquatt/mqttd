package plantae.citrus.exercise

import akka.actor.Actor
import akka.io.Tcp.{PeerClosed, Write, Received}

class EchoHandler extends Actor{

  def receive = {
    case Received(data) =>
      println(data)
      sender() ! Write(data)
      // ByteString으로 들어온다.
    case PeerClosed => context.stop(self)
  }
}
