package plantae.citrus.mqtt.actors

import akka.actor.{Props, ActorRef, Actor}
import akka.actor.Actor.Receive
import akka.event.Logging
import plantae.citrus.mqtt.dto.STRING
import scala.collection.mutable.Map


case class TopicDirectoryReq(topicName: STRING)

case class TopicDirectoryResp(topicName: STRING, actor: Option[ActorRef])

case class TopicDirectoryRemove(topicName: STRING)

class TopicDirectory extends Actor {

  private val logger = Logging(context.system, this)

  val topicMap: Map[String, ActorRef] = Map()

  def receive = {
    case TopicDirectoryReq(topicName) => {
      logger.info("Topic directory request({})", topicName)
      if (topicMap.contains(topicName.value)){
        sender ! TopicDirectoryResp(topicName, topicMap.get(topicName.value))
      } else {
        val topicActor = ActorContainer.system.actorOf(Props(new Topic(topicName.value)), topicName.value)
        topicMap.+=((topicName.value, topicActor))
        sender ! TopicDirectoryResp(topicName, Some(topicActor))
      }
    }
    case TopicDirectoryRemove(topicName) => {
      logger.info("Topic directory remove({})", topicName.value)
    }
  }

}
