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

case class DirectorySessionResult(name: String, actor: ActorRef) extends DirectoryOperation

case class DirectoryTopicResult(name: String, actors: List[ActorRef]) extends DirectoryOperation

sealed trait ActorType

case object TypeSession extends ActorType

case object TypeTopic extends ActorType

class DirectoryProxy extends Actor with ActorLogging {

  var router = {
    Router(RoundRobinRoutingLogic(), Vector.fill(5) {
      val r = context.actorOf(Props[Directory])
      context watch r
      ActorRefRoutee(r)
    })
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
  implicit val timeout = akka.util.Timeout(2, TimeUnit.SECONDS)
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  def receive = {
    case DirectoryReq(name, actorType) => {
      actorType match {
        case TypeSession =>
          sender ! DirectorySessionResult(name,
            Await.result(
              SystemRoot.sessionRoot ? name, Duration.Inf).asInstanceOf[ActorRef])
        case TypeTopic =>
          sender ! DirectoryTopicResult(name,
            Await.result(
              SystemRoot.topicRoot ? name, Duration.Inf).asInstanceOf[List[ActorRef]])
      }
    }
  }
}
