package plantae.citrus.mqtt.actors.topic

import akka.actor._
import plantae.citrus.exercise.DiskTreeNode

import scala.collection.mutable.Map
import scala.util.Random

sealed trait TopicRequest

sealed trait TopicResponse

case class Subscribe(clientId: String) extends TopicRequest

case class Unsubscribe(clientId: String) extends TopicRequest

case object ClearList extends TopicRequest

case class TopicInMessage(payload: Array[Byte], qos: Short, retain: Boolean, packetId: Option[Int]) extends TopicRequest

case object TopicInMessageAck extends TopicResponse

case class TopicOutMessage(payload: Array[Byte], qos: Short, retain: Boolean, topic: String) extends TopicResponse

case object TopicOutMessageAck extends TopicRequest

class TopicRoot extends Actor with ActorLogging {

  val root = DiskTreeNode[ActorRef]("", "", Map[String, DiskTreeNode[ActorRef]]())

  override def receive = {
    case topicName: String => {
      log.info("Topic root {}", topicName)
      root.getNodes(topicName) match {
        case Nil => {
          log.debug("new topic is created[{}]", topicName)
          val topic = context.actorOf(Props(classOf[Topic], topicName), Random.alphanumeric.take(128).mkString)
          root.addNode(topicName, topic)
          sender ! List(topic)
        }
        case topics : List[ActorRef] => sender ! topics

      }
//      context.child(topicName) match {
//        case Some(x) => sender ! x
//        case None => log.debug("new topic is created [{}]", topicName)
//          sender ! context.actorOf(Props[Topic], topicName)
//      }
    }
  }
}

class Topic(name: String) extends Actor with ActorLogging {

  val subscriberMap: Map[String, ActorRef] = Map()

  def receive = {
//    case Terminated(a) => {
//
//    }
    case Subscribe(clientId) => {
      log.info("Subscribe client({}) topic({})", clientId, name)
      subscriberMap.+=((clientId, sender))
//      context.watch(sender())
      printEverySubscriber
    }

    case Unsubscribe(clientId) => {
      log.debug("Unsubscribe client({}) topic({})", clientId, name)
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
        (actor) => actor ! TopicOutMessage(payload, qos, retain, name)
      )
    }
    case TopicOutMessageAck =>
  }

  def printEverySubscriber = {
    log.info("{}'s subscriber ", name)
    subscriberMap.foreach(s => log.info("{},", s._1))
  }
}
