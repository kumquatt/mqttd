package plantae.citrus.mqtt.actors.directory

import akka.actor._
import plantae.citrus.mqtt.actors._

sealed trait DirectoryOperation

case class Register(actorName: String, actorType: ActorType) extends DirectoryOperation

case class RegisterAck(actorName: String) extends DirectoryOperation

case class Remove(actorName: String, actorType: ActorType) extends DirectoryOperation

case class RemoveAck(actorName: String) extends DirectoryOperation

case class DirectoryReq(actorName: String, actorType: ActorType) extends DirectoryOperation

case class DirectoryResp(actorName: String, actor: ActorRef) extends DirectoryOperation

sealed trait ActorType

case object TypeSession extends ActorType

case object TypeTopic extends ActorType

trait DirectoryMonitorActor extends Actor with ActorLogging {
  override def preStart = {
    ActorContainer.directoryOperation(Register(self.path.name, actorType), context, self)
  }

  override def postStop = {
    //    ActorContainer.directoryOperation(Remove(self.path.name, actorType), context, self)
  }

  def actorType: ActorType
}

class Directory extends Actor with ActorLogging {
  var sessionActorMap: Map[String, ActorRef] = Map()
  var topicActorMap: Map[String, ActorRef] = Map()

  def receive = {
    case Register(name, actorType) => {
      log.info("register actor : " + name + "\t" + sender.path)
      context.watch(sender)
      actorType match {
        case TypeSession => sessionActorMap = sessionActorMap.updated(name, sender)
        case TypeTopic => topicActorMap = topicActorMap.updated(name, sender)
      }
      sender() ! RegisterAck(name)
    }

    case Remove(name, actorType) => {
      log.info("remove actor : " + name + "\t" + sender.path)
      context.unwatch(sender)

      actorType match {
        case TypeSession => sessionActorMap = sessionActorMap - name
        case TypeTopic => topicActorMap = topicActorMap - name
      }
      sender() ! RemoveAck(name)
    }

    case DirectoryReq(name, actorType) => {
      val originalSender = sender;

      val returnActor = context.actorOf(Props(new Actor {
        def receive = {
          case newActor: ActorRef =>
            originalSender ! DirectoryResp(name, newActor)
            context.stop(self)
        }
      }))
      actorType match {
        case TypeSession => sessionActorMap.get(name) match {
          case Some(x) => {
            log.info("load exist actor : {}", x.path)
            returnActor ! x
          }
          case None => ActorContainer.sessionCreator.tell(name, returnActor)
        }

        case TypeTopic => topicActorMap.get(name) match {
          case Some(x) => {
            log.info("load exist actor : {}", x.path)
            returnActor ! x
          }
          case None => ActorContainer.topicCreator.tell(name, returnActor)
        }

      }

    }
    case Terminated(x) => {
      log.info("Terminated actor({})", x.path)
      x.path.parent.name match {
        case "session" =>
          sessionActorMap.foreach(each => if (each._2 == x) {
            println(each._1 + "is terminated")
            self ! Remove(each._1, TypeSession)
          })
        case other =>
          topicActorMap.foreach(each => if (each._2 == x) {
            println(each._1 + "is terminated")
            self ! Remove(each._1, TypeTopic)
          })

      }
    }
  }

}
