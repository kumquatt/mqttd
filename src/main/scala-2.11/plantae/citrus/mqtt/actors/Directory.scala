package plantae.citrus.mqtt.actors

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import plantae.citrus.mqtt.dto.connect.CONNECT


case class Register(actorName: String, senderOfSender: ActorRef, sendOfSenderOfSender: ActorRef, connect: CONNECT)

case class Remove(actorName: String, actor: ActorRef, sender: ActorRef)

case class RegisterAck(actorName: String, senderOfSender: ActorRef, sendOfSenderOfSender: ActorRef, connect: CONNECT)

case class RemoveAck(actorName: String, sender: ActorRef)

case class DirectoryReq(actorName: String)

case class DirectoryResp(actorName: String, actor: Option[ActorRef])

class Directory extends Actor {
  private val logger = Logging(context.system, this)

  var validActorMap: Map[String, ActorRef] = Map()

  def receive = {
    case Register(actorName, senderOfSender, sendOfSenderOfSender, connect) => {
      logger.info("register actor : " + sender)
      validActorMap = validActorMap.updated(actorName, sender)
      sender() ! RegisterAck(actorName, senderOfSender, sendOfSenderOfSender, connect)
      validActorMap.foreach {
        logger.info("{}",_)
      }
    }
    case Remove(actorName, actor, senderOfSender) => {
      logger.info("remove actor : " + sender)
      validActorMap = {
        if (validActorMap contains (actorName)) validActorMap - actorName
        else validActorMap
      }
      sender() ! RemoveAck(actorName, senderOfSender)
    }
    case DirectoryReq(actorName) => {
      logger.info("directory request " + actorName)
      sender ! DirectoryResp(actorName, validActorMap.get(actorName))
    }
  }

}
