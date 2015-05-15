package plantae.citrus.mqtt.actors.topic

import akka.actor._
import plantae.citrus.mqtt.actors.session.PublishMessage
import scodec.bits.ByteVector

import scala.collection.mutable.Map
import scala.util.Random

case class TopicSubscribe(session: ActorRef, qos: Short, reply: Boolean = true)

case class TopicSubscribed(topicName: String, result: Boolean, newbie: Boolean = false, session: ActorRef)

case class TopicUnsubscribe(session: ActorRef)

case class TopicUnsubscribed(topicName: String, result: Boolean)

case object TopicSubscriberClear

case object TopicGetSubscribers

case class TopicSubscribers(subscribers: List[(ActorRef, Short)])

//case class Publish(topic: String, payload: ByteVector, retain: Boolean, packetId: Option[Int]) extends TopicRequest
case class TopicStoreRetainMessage(payload: ByteVector)

case class TopicPublishRetainMessage(session: ActorRef)


object Topic {
  def props(topicName: String) = {
    Props(classOf[Topic], topicName)
  }
}

class Topic(topicName: String) extends Actor with ActorLogging {
  private val subscriberMap: collection.mutable.HashMap[ActorRef, Short] = collection.mutable.HashMap[ActorRef, Short]()
  private val retainMessageSet: collection.mutable.Set[ByteVector] = collection.mutable.Set[ByteVector]()

  def receive = {

    case TopicSubscribe(session, qos, reply) =>
      log.debug("[NEWTOPIC]TopicSubscribe topic({}) client({}) qos({})", topicName, session.path.name, qos)
      if (!subscriberMap.contains(session)) {
        subscriberMap.+=((session, qos))


        if (reply)
          sender ! TopicSubscribed(topicName, true, true, session)
      }
      else {
        if (subscriberMap.get(session).get < qos) {
          subscriberMap.-=(session)
          subscriberMap.+=((session, qos))
        }


        if (reply)
          sender ! TopicSubscribed(topicName, true, session = session)
      }
    case TopicUnsubscribe(session) =>
      log.debug("[NEWTOPIC]TopicUnsubscribe topic({}) client({})", topicName, session.path.name)
      subscriberMap.-=(session)
//      sender ! TopicUnsubscribed(topicName, true)
    case TopicSubscriberClear =>
      log.debug("[NEWTOPIC]TopicSubscriberClear topic({})", topicName)
      subscriberMap.clear
    case TopicGetSubscribers =>
      sender ! TopicSubscribers(subscriberMap.toList)
    case message: TopicStoreRetainMessage => {
      log.debug("RetainMessage Topic({}) Store retain message {}", topicName, message)
      retainMessageSet.clear()
      if (message.payload.size != 0)
        retainMessageSet.add(message.payload)
    }

    case TopicPublishRetainMessage(session) => {
      log.debug("PublishRetain retainMessage({}) topicName({}) session({})", retainMessageSet, topicName, session)
      if (retainMessageSet.size > 0)
        session ! PublishMessage(topicName, 2, retainMessageSet.head)
    }

  }
}

trait NTopic {
  val children: Map[String, TopicNode2] = Map[String, TopicNode2]()

  def pathToList(path: String): List[String] = {
    path.split("/").toList
  }


}

case class TopicNode2(name: String, elem: ActorRef, context: ActorRefFactory, root: Boolean = false) extends NTopic {
  def getTopicNode(path: String): ActorRef = {
    getTopicNode(pathToList(path))
  }

  def getTopicNode(paths: List[String]): ActorRef = {
    paths match {
      case Nil => elem
      case x :: Nil => {
        val node: TopicNode2 = children.get(x) match {
          case Some(node) => node
          case None => {
            val newNodeName = if (root) x else name + "/" + x
            val newTopicActor = context.actorOf(Topic.props(newNodeName), Random.alphanumeric.take(128).mkString)
            val node = TopicNode2(name = newNodeName, elem = newTopicActor, context = context)

            children.+=((x, node))

            node
          }
        }
        node.elem
      }
      case x :: others => {
        val node: TopicNode2 = children.get(x) match {
          case Some(node) => node
          case None => {
            val newNodeName = if (root) x else name + "/" + x
            val newTopicActor = context.actorOf(Topic.props(newNodeName), Random.alphanumeric.take(128).mkString)
            val node = TopicNode2(name = newNodeName, elem = newTopicActor, context = context)
            children.+=((x, node))

            node
          }
        }
        node.getTopicNode(others)
      }
    }
  }

  def matchedTopicNodes(path: String): List[ActorRef] = {
    matchedTopicNodes(pathToList(path))
  }

  def matchedTopicNodes(paths: List[String]): List[ActorRef] = {
    paths match {
      case Nil => List(elem)
      case "+" :: Nil => {
        children.map(x => x._2.elem).toList
      }
      case "#" :: Nil => {
        getEveryElements()
      }
      case x :: Nil => {
        children.get(x) match {
          case Some(nodes) => nodes.elem :: Nil
          case None => List()
        }
      }
      case "+" :: others => {
        children.map(x => x._2.matchedTopicNodes(others)).flatten.toList
      }
      case "#" :: others => {
        getEveryElements()
      }
      case x :: others => {
        children.get(x) match {
          case Some(node) => node.matchedTopicNodes(others)
          case None => List()
        }
      }

    }

  }

  def getEveryElements() : List[ActorRef] = {

    val childrenNodes = children.map( x => {
      x._2.getEveryElements()
    }).flatten.toList

    if (!root) elem :: childrenNodes else childrenNodes
  }

  def matchedTopicNodesWithOutWildCard(path: String): List[ActorRef] = {
    // you must not use wildcard in here
    if (path.contains("+") || path.contains("#")) List()
    else matchedTopicNodesWithOutWildCard(pathToList(path))
  }


  def matchedTopicNodesWithOutWildCard(paths: List[String]): List[ActorRef] = {
    paths match {
      case Nil => List(elem)
      case x :: Nil => {
        val l1 = children.get(x) match {
          case Some(node) => node.elem :: Nil
          case None => Nil
        }

        val l2 = children.get("+") match {
          case Some(node) => node.elem :: Nil
          case None => Nil
        }

        val l3 = children.get("#") match {
          case Some(node) => node.elem :: Nil
          case None => Nil
        }

        l1 ::: l2 ::: l3
      }
      case x :: others => {
        val l1 = children.get(x) match {
          case Some(node) => node.matchedTopicNodesWithOutWildCard(others)
          case None => Nil
        }

        val l2 = children.get("+") match {
          case Some(node) => node.matchedTopicNodesWithOutWildCard(others)
          case None => Nil
        }

        val l3 = children.get("#") match {
          case Some(node) => node.elem :: Nil
          case None => Nil
        }

        l1 ::: l2 ::: l3
      }
    }
  }
}

object Test extends App {
  val root = TopicNode2("", null, ActorSystem(), true)

  root.getTopicNode("a/1")
  root.getTopicNode("a/2")
  root.getTopicNode("a/3")

  println(root.matchedTopicNodes("+/+"))

}