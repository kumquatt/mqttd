package plantae.citrus.mqtt.actors.directory

import akka.actor._
import akka.event.Logging
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import plantae.citrus.mqtt.actors.ActorContainer

case object GetDirectoryActor

case class DirectoryActor(actor: ActorRef)

class DirectoryProxyMaster extends Actor with ActorLogging {

  var router = {
    val routees = Vector.fill(5) {
      val r = context.actorOf(Props[DirectoryProxyWorker])
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case GetDirectoryActor => router.route(GetDirectoryActor, sender)

    case Terminated(a) =>
      router = router.removeRoutee(a)
      val r = context.actorOf(Props[DirectoryProxyWorker])
      context watch r
      router = router.addRoutee(r)
  }
}

class DirectoryProxyWorker extends Actor with ActorLogging {

  def receive = {
    case GetDirectoryActor =>
      val actor = ActorContainer.directory
      sender() ! DirectoryActor(actor)
  }
}
