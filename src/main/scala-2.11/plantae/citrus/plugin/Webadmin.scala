package plantae.citrus.plugin

import akka.actor.{Actor, ActorLogging}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import plantae.citrus.mqtt.actors.SystemRoot
import spray.can.Http
import scala.concurrent.duration._
import spray.routing._
import spray.http._
import MediaTypes._

class WebPlugin extends Actor with WebAdmin with ActorLogging {

  implicit val system = SystemRoot.system
  implicit val timeout = Timeout(5.seconds)

  IO(Http) ? Http.Bind(self, interface = SystemRoot.config.getString("mqtt.webadmin.hostname"), port = SystemRoot.config.getInt("mqtt.webadmin.port"))

  def actorRefFactory = context

  def receive = runRoute(myRoute)
}

trait WebAdmin extends HttpService {

  val myRoute =
    path("") {
      get {
        respondWithMediaType(`text/html`) {
          // XML is marshalled to `text/xml` by default, so we simply override here
          complete {
            <html>
              <body>
                <h1>Say hello to
                  <i>spray-routing</i>
                  on
                  <i>spray-can</i>
                  !</h1>
              </body>
            </html>
          }
        }
      }
    }
}
