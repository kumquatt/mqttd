package plantae.citrus.exercise

import akka.actor.Actor
import akka.io.Tcp.{PeerClosed, Write, Received}

class EchoHandler extends Actor{

  def receive = {
    case Received(data) => sender() ! Write(data)
    case PeerClosed => context.stop(self)
  }
}
