package plantae.citrus.mqtt.actors.connection

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, Props}
import akka.io.{IO, Tcp}
import plantae.citrus.mqtt.actors.SystemRoot

class Server extends Actor with ActorLogging {

  import Tcp._
  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress(
    SystemRoot.config.getString("mqtt.broker.hostname"),
    SystemRoot.config.getInt("mqtt.broker.port"))
    , backlog = 1023)

  def receive = {
    case Bound(localAddress) =>

    case CommandFailed(_: Bind) =>
      log.error("bind failure")
      context stop self

    case Connected(remote, local) =>
      log.info("new connection" + remote)
      sender ! Register(context.actorOf(Props(classOf[PacketBridge], sender)))
  }
}
