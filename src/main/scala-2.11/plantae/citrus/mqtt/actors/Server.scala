package plantae.citrus.mqtt.actors

import java.net.InetSocketAddress

import akka.actor.{Actor, Props}
import akka.io.{IO, Tcp}
import plantae.citrus.exercise.StartUpMessage

object Server extends App {
  val actor = ActorContainer.system.actorOf(Props[Server], name = "broker")
}

class Server extends Actor {

  import Tcp._
  import akka.util.Timeout
  import context.system

  import scala.concurrent.ExecutionContext

  implicit val ec = ExecutionContext.global

  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 8888))

  implicit val timeout = Timeout(5, java.util.concurrent.TimeUnit.SECONDS)

  def receive = {
    case StartUpMessage(name) =>

    case b@Bound(localAddress) =>

    case CommandFailed(_: Bind) => context stop self

    case c@Connected(remote, local) =>
      println("new connection" + remote)
      val handler = context.actorOf(Props[PacketBridge])
      val connection = sender()
      connection ! Register(handler)

  }
}
