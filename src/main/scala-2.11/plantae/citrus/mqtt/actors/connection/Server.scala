package plantae.citrus.mqtt.actors.connection

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, Props}
import akka.io.{IO, Tcp}
import plantae.citrus.mqtt.actors.SystemRoot

class Server extends Actor with ActorLogging {

  import Tcp._
  import context.system

  import scala.concurrent.ExecutionContext

  implicit val ec = ExecutionContext.global


  IO(Tcp) ! Bind(self, new InetSocketAddress(
    SystemRoot.config.getString("mqtt.broker.hostname"),
    SystemRoot.config.getInt("mqtt.broker.port")))

  def receive = {
    case Bound(localAddress) =>

    case CommandFailed(_: Bind) => context stop self

    case Connected(remote, local) =>
      log.info("new connection" + remote)
      sender ! Register(context.actorOf(Props(classOf[PacketBridge], sender)))
  }
}
