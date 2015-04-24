package plantae.citrus.mqtt.actors.directory

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.pattern.ask
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import plantae.citrus.mqtt.actors._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

sealed trait DirectoryOperation

case class DirectoryReq(name: String, actorType: ActorType) extends DirectoryOperation

case class DirectoryResp(name: String, actor: ActorRef) extends DirectoryOperation

sealed trait ActorType

case object TypeSession extends ActorType

case object TypeTopic extends ActorType

class DirectoryProxy extends Actor with ActorLogging {

  var router = {
    val routees = Vector.fill(5) {
      val r = context.actorOf(Props[Directory])
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case request: DirectoryReq => router.route(request, sender)

    case Terminated(a) =>
      router = router.removeRoutee(a)
      val r = context.actorOf(Props[Directory])
      context watch r
      router = router.addRoutee(r)
  }
}

class Directory extends Actor with ActorLogging {
  implicit val timeout = akka.util.Timeout(5, TimeUnit.SECONDS)
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  def receive = {
    case DirectoryReq(name, actorType) => {
      sender ! DirectoryResp(name, actorType match {
        case TypeSession => Await.result(ActorContainer.sessionRoot ? name, Duration.Inf).asInstanceOf[ActorRef]
        case TypeTopic => Await.result(ActorContainer.topicRoot ? name, Duration.Inf).asInstanceOf[ActorRef]
      }
      )
    }
  }

}
