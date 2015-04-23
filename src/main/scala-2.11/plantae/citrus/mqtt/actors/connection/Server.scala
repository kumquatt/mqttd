package plantae.citrus.mqtt.actors.connection

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, Props}
import akka.io.{IO, Tcp}

class Server extends Actor with ActorLogging {

  import Tcp._
  import akka.util.Timeout
  import context.system

  import scala.concurrent.ExecutionContext

  implicit val ec = ExecutionContext.global

  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 8888))

  implicit val timeout = Timeout(5, java.util.concurrent.TimeUnit.SECONDS)

  def receive = {
    case b@Bound(localAddress) =>

    case CommandFailed(_: Bind) => context stop self

    case c@Connected(remote, local) =>
      log.info("new connection" + remote)
      val handler = context.actorOf(Props[PacketBridge])
      val connection = sender()
      connection ! Register(handler)

  }
}
