package plantae.citrus.mqtt.actors

import akka.actor.{Actor, ActorRef}
import akka.event.Logging

import scala.collection.mutable.Map

case class Subscribe(clientId: String)

case class Unsubscribe(clientId: String)

case object ClearList

case class TopicMessage(msg: String)

class Topic(topicName: String) extends Actor {

  private val logger = Logging(context.system, this)

  val subscriberMap: Map[String, ActorRef] = Map()

  def receive = {
    case Subscribe(clientId) => {
      logger.info("Subscribe client({}) topic({})", clientId, topicName)
      subscriberMap.+=((clientId, sender))
      printEverySubscriber
    }

    case Unsubscribe(clientId) => {
      logger.info("Unsubscribe client({}) topic({})", clientId, topicName)
      subscriberMap.-(clientId)
      printEverySubscriber
    }

    case ClearList => {
      logger.info("Clear subscriber list")
      subscriberMap.clear()
      printEverySubscriber
    }

    case TopicMessage(msg) => {
    }
  }

  def printEverySubscriber = {
    logger.info("{}'s subscriber ", topicName)
    subscriberMap.foreach(s => logger.info("{},", s._1))
  }
}
