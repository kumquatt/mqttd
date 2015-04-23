package plantae.citrus.mqtt.actors

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.event.Logging


case class Register(actorName: String, actorType: ActorType)

case class RegisterAck(actorName: String)

case class Remove(actorName: String, actorType: ActorType)

case class RemoveAck(actorName: String)

case class DirectoryReq(actorName: String, actorType: ActorType)

case class DirectoryResp(actorName: String, actor: ActorRef)

sealed trait ActorType

case object TypeSession extends ActorType

case object TypeTopic extends ActorType

trait DirectoryMonitorActor extends Actor {
  override def preStart = {
    ActorContainer.directory ! Register(self.path.name, actorType)
  }

  override def postStop = {
    ActorContainer.directory ! Remove(self.path.name, actorType)
  }

  def actorType: ActorType
}

class Directory extends Actor {
  private val logger = Logging(context.system, this)
  var sessionActorMap: Map[String, ActorRef] = Map()
  var topicActorMap: Map[String, ActorRef] = Map()

  def receive = {
    case Register(name, actorType) => {
      logger.info("register actor : " + name + "\t" + sender.path)
      context.watch(sender)
      actorType match {
        case TypeSession => sessionActorMap = sessionActorMap.updated(name, sender)
        case TypeTopic => topicActorMap = topicActorMap.updated(name, sender)
      }
      sender() ! RegisterAck(name)
    }

    case Remove(name, actorType) => {
      logger.info("remove actor : " + name + "\t" + sender.path)
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
            logger.info("load exist actor : {}", x.path)
            returnActor ! x
          }
          case None => ActorContainer.sessionCreator.tell(name, returnActor)
        }

        case TypeTopic => topicActorMap.get(name) match {
          case Some(x) => {
            logger.info("load exist actor : {}", x.path)
            returnActor ! x
          }
          case None => ActorContainer.topicCreator.tell(name, returnActor)
        }

      }

    }
    case Terminated(x) => {
      println(x.path)
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
