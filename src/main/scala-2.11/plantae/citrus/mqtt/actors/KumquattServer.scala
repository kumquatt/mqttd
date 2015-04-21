package plantae.citrus.mqtt.actors

import java.net.InetSocketAddress

import akka.actor.{Props, Actor}
import akka.io.{IO, Tcp}
import plantae.citrus.exercise.StartUpMessage


class KumquattServer extends Actor {
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