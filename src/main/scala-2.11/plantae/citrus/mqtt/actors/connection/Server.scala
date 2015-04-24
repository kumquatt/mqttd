package plantae.citrus.mqtt.actors.connection

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, Props}
import akka.io.{IO, Tcp}
import com.typesafe.config.ConfigFactory

class Server extends Actor with ActorLogging {

  import Tcp._
  import akka.util.Timeout
  import context.system

  import scala.concurrent.ExecutionContext

  implicit val ec = ExecutionContext.global

  val config = ConfigFactory.load()

  IO(Tcp) ! Bind(self, new InetSocketAddress(config.getString("mqtt.broker.hostname"), config.getInt("mqtt.broker.port")))

  implicit val timeout = Timeout(5, java.util.concurrent.TimeUnit.SECONDS)

  def receive = {
    case Bound(localAddress) =>

    case CommandFailed(_: Bind) => context stop self

    case Connected(remote, local) =>
      log.info("new connection" + remote)
      val handler = context.actorOf(Props[PacketBridge])
      val connection = sender()
      connection ! Register(handler)
  }
}
