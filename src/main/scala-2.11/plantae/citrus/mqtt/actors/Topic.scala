package plantae.citrus.mqtt.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging

import scala.collection.mutable.Map

case class Subscribe(clientId: String)

case class Unsubscribe(clientId: String)

case object ClearList

case class TopicMessage(payload: Array[Byte], qos: Int, retain: Boolean)

class TopicCreator extends Actor {
  override def receive = {
    case topicName: String => sender ! context.actorOf(Props[Topic], topicName)
  }
}

class Topic extends DirectoryMonitorActor {

  private val logger = Logging(context.system, this)

  val subscriberMap: Map[String, ActorRef] = Map()

  def receive = {
    case Subscribe(clientId) => {
      logger.info("Subscribe client({}) topic({})", clientId, self.path.name)
      subscriberMap.+=((clientId, sender))
      printEverySubscriber
    }

    case Unsubscribe(clientId) => {
      logger.info("Unsubscribe client({}) topic({})", clientId, self.path.name)
      subscriberMap.-(clientId)
      printEverySubscriber
    }

    case ClearList => {
      logger.info("Clear subscriber list")
      subscriberMap.clear()
      printEverySubscriber
    }

    case TopicMessage(payload, qos, retain) => {
      logger.info("qos : {} , retain : {} , payload : {}", qos, retain, new String(payload))
    }
  }

  def printEverySubscriber = {
    logger.info("{}'s subscriber ", self.path.name)
    subscriberMap.foreach(s => logger.info("{},", s._1))
  }

  override def actorType: ActorType = TOPIC
}
