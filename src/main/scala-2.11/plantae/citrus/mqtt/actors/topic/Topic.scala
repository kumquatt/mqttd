package plantae.citrus.mqtt.actors.topic

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import plantae.citrus.mqtt.actors.directory.{ActorType, DirectoryMonitorActor, TypeTopic}

import scala.collection.mutable.Map

case class Subscribe(clientId: String)

case class Unsubscribe(clientId: String)

case object ClearList

case class TopicInMessage(payload: Array[Byte], qos: Short, retain: Boolean, packetId: Option[Int])

case object TopicInMessageAck

case class TopicOutMessage(payload: Array[Byte], qos: Short, retain: Boolean, topic: String)

case object TopicOutMessageAck

class TopicCreator extends Actor with ActorLogging {

  override def receive = {
    case topicName: String => {
      log.debug("new topic is created [{}]", topicName)
      sender ! context.actorOf(Props[Topic], topicName)
    }
  }
}

class Topic extends DirectoryMonitorActor with ActorLogging {

  val subscriberMap: Map[String, ActorRef] = Map()

  def receive = {
    case Subscribe(clientId) => {
      log.info("Subscribe client({}) topic({})", clientId, self.path.name)
      subscriberMap.+=((clientId, sender))
      printEverySubscriber
    }

    case Unsubscribe(clientId) => {
      log.debug("Unsubscribe client({}) topic({})", clientId, self.path.name)
      subscriberMap.-(clientId)
      printEverySubscriber
    }

    case ClearList => {
      log.debug("Clear subscriber list")
      subscriberMap.clear()
      printEverySubscriber
    }

    case TopicInMessage(payload, qos, retain, packetId) => {
      log.debug("qos : {} , retain : {} , payload : {} , sender {}", qos, retain, new String(payload), sender)
      sender ! TopicInMessageAck
      subscriberMap.values.foreach(
        (actor) => actor ! TopicOutMessage(payload, qos, retain, self.path.name)
      )
    }
    case TopicOutMessageAck =>
  }

  def printEverySubscriber = {
    log.info("{}'s subscriber ", self.path.name)
    subscriberMap.foreach(s => log.info("{},", s._1))
  }

  override def actorType: ActorType = TypeTopic
}
