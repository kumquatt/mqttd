package plantae.citrus.exercise

import java.net.InetSocketAddress
import akka.actor.{Props, ActorSystem, Actor}
import akka.io.{IO, Tcp}
import com.typesafe.config.ConfigFactory


object EchoServer extends App{
  println("Hello world")
  val system = ActorSystem("AkkaEcho", ConfigFactory.load.getConfig("echoserver"))
  val server = system.actorOf(Props[EchoServer], name = "echoserver")
  server ! StartUpMessage

}

class EchoServer extends Actor {
  import context.system
  import Tcp._

  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 8888))

  def receive = {
    case StartUpMessage =>
      println("startUp")

    case b @ Bound(localAddress) =>

    case CommandFailed(_: Bind) => context stop self

    case c @ Connected(remote, local) =>
        println("connected!!!")
  }
}
