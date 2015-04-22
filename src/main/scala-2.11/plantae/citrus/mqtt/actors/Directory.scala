package plantae.citrus.mqtt.actors

import akka.actor.{Actor, ActorRef, Terminated}
import akka.event.Logging


case class Register(actorName: String, actorType: ActorType)

case class RegisterAck(actorName: String)

case class Remove(actorName: String, actorType: ActorType)

case class RemoveAck(actorName: String)

case class DirectoryReq(actorName: String, actorType: ActorType)

case class DirectoryResp(actorName: String, actor: Option[ActorRef])

class ActorType

case object SESSION extends ActorType

case object TOPIC extends ActorType

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
        case SESSION => sessionActorMap = sessionActorMap.updated(name, sender)
        case TOPIC => topicActorMap = topicActorMap.updated(name, sender)
      }
      sender() ! RegisterAck(name)
    }

    case Remove(name, actorType) => {
      logger.info("remove actor : " + name + "\t" + sender.path)
      context.unwatch(sender)

      actorType match {
        case SESSION => sessionActorMap = sessionActorMap - name
        case TOPIC => topicActorMap = topicActorMap - name
      }
      sender() ! RemoveAck(name)
    }

    case DirectoryReq(name, actorType) => {
      logger.info("directory request : " + name)
      sender ! DirectoryResp(name, actorType match {
        case SESSION => sessionActorMap.get(name)
        case TOPIC => topicActorMap.get(name)
      }
      )
    }
    case Terminated(x) => {
      println(x.path)
      x.path.parent.name match {
        case "session" =>
          sessionActorMap.foreach(each => if (each._2 == x) {
            println(each._1 + "is terminated")
            self ! Remove(each._1, SESSION)
          })
        case other =>
          topicActorMap.foreach(each => if (each._2 == x) {
            println(each._1 + "is terminated")
            self ! Remove(each._1, TOPIC)
          })

      }
    }
  }

}
