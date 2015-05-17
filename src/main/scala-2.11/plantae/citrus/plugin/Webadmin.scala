package plantae.citrus.plugin

import plantae.citrus.mqtt.actors.SystemRoot
import spray.http.MediaTypes

import scala.concurrent.duration._

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import spray.httpx.Json4sSupport
import spray.routing._
import spray.can.server.Stats
import spray.http.StatusCodes._
import spray.httpx.marshalling.Marshaller
import MediaTypes._


class WebPlugin extends Actor with WebAdmin with ActorLogging {

  implicit val system = SystemRoot.system
  implicit val timeout2 = Timeout(5 seconds)

  IO(Http) ? Http.Bind(self, interface = SystemRoot.config.getString("mqtt.webadmin.hostname"), port = SystemRoot.config.getInt("mqtt.webadmin.port"))

  def actorRefFactory = context

  def receive = runRoute(myRoute)
}

case class Foo(bar: String)

trait WebAdmin extends HttpService {
  import WorkerActor._

  implicit def executionContext = actorRefFactory.dispatcher
  implicit val timeout = Timeout(5 seconds)

  val worker = actorRefFactory.actorOf(Props[WorkerActor], "worker")

  val myRoute =

    path("") {
      get {
        respondWithMediaType(`text/html`) {
          complete {
            getStatus
          }
        }
      }
    }

  def getStatus = {
     {(worker ? "status").mapTo[Ok].map(result => s"<html><body>sessions : ${result.sessions}</br> " +
       s"connections : ${result.connections}</br> " +
       s"topics : ${result.topics}</body></html>" ).recover{case _ => "error in get topics"}}
  }

}

object WorkerActor {
  trait WorkerResult
  case class Ok(sessions: Int, connections: Int, topics: Int ) extends WorkerResult
  case class Error(reason: String) extends WorkerResult
}

class WorkerActor extends Actor with ActorLogging {
  import WorkerActor._

  def receive = {
    case "status" => {
      log.info("ask session!!!!")
      sender ! Ok(util.Random.nextInt(1000), util.Random.nextInt(1000), util.Random.nextInt(1000))
    }
    case _ => Error("no method")
  }
}

