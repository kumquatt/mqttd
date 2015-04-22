package plantae.citrus.mqtt.actors

import akka.actor.{Actor, ActorRef}
import akka.event.Logging


case class Register(actorName: String)

case class RegisterAck(actorName: String)

case class Remove(actorName: String)

case class RemoveAck(actorName: String)

case class DirectoryReq(actorName: String)

case class DirectoryResp(actorName: String, actor: Option[ActorRef])

class Directory extends Actor {
  private val logger = Logging(context.system, this)
  var validActorMap: Map[String, ActorRef] = Map()

  def receive = {
    case Register(name) => {
      logger.info("register actor : " + name + sender)
      validActorMap = validActorMap.updated(name, sender)
      sender() ! RegisterAck(name)
    }

    case Remove(name) => {
      logger.info("remove actor : " + sender)
      validActorMap = {
        if (validActorMap contains (name)) validActorMap - name
        else validActorMap
      }
      sender() ! RemoveAck(name)
    }

    case DirectoryReq(name) => {
      logger.info("directory request " + name)
      sender ! DirectoryResp(name, validActorMap.get(name))
    }
  }

}
