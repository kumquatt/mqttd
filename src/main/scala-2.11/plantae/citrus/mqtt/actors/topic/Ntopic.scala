package plantae.citrus.mqtt.actors.topic

import akka.actor._
import plantae.citrus.mqtt.actors.session.PublishMessage
import plantae.citrus.mqtt.packet.{FixedHeader, PublishPacket}
import scodec.bits.ByteVector
import scala.collection.mutable.Map
import scala.collection.mutable.Set
import scala.util.Random

case class TopicSubscribe(session: ActorRef, qos: Short, reply: Boolean = true)

case class TopicSubscribed(topicName: String, result: Boolean)

case class TopicUnsubscribe(session: ActorRef)

case class TopicUnsubscribed(topicName: String, result: Boolean)

case object TopicSubscriberClear

//case class TopicMessagePublish(payload: ByteVector, retain: Boolean, packetId: Option[Int], wildcardSessions: List[(ActorRef, Short)])
//case object TopicMessagePublished
case object TopicGetSubscribers

case class TopicSubscribers(subscribers: List[(ActorRef, Short)])

object Topic {
  def props(topicName: String) = {
    Props(classOf[Topic], topicName)
  }
}

class Topic(topicName: String) extends Actor with ActorLogging {
  private val subscriberMap: collection.mutable.HashMap[ActorRef, Short] = collection.mutable.HashMap[ActorRef, Short]()

  def receive = {

    case TopicSubscribe(session, qos, reply) =>
      log.debug("[NEWTOPIC]TopicSubscribe topic({}) client({}) qos({})", topicName, session.path.name, qos)
      if (!subscriberMap.contains(session)) subscriberMap.+=((session, qos))
      else {
        if (subscriberMap.get(session).get < qos) {
          subscriberMap.-=(session)
          subscriberMap.+=((session, qos))
        }
      }
      if (reply)
        sender ! TopicSubscribed(topicName, true)
    case TopicUnsubscribe(session) =>
      log.debug("[NEWTOPIC]TopicUnsubscribe topic({}) client({})", topicName, session.path.name)
      subscriberMap.-=(session)
      sender ! TopicUnsubscribed(topicName, true)
    case TopicSubscriberClear =>
      log.debug("[NEWTOPIC]TopicSubscriberClear topic({})", topicName)
      subscriberMap.clear
    //    case message: TopicMessagePublish =>
    //      log.debug("[NEWTOPIC]TopiceMessagePublish topic({}) payload({}) message({})", topicName, new String(message.payload.toArray), message)
    //
    //      val ns = collection.mutable.HashMap[ActorRef, Short]() ++ subscriberMap
    //
    //      message.wildcardSessions.foreach(x => {
    //        ns.get(x._1) match {
    //          case Some(qos) =>
    //            if (qos < x._2) {
    //              ns.remove(x._1)
    //              ns.+=((x._1, x._2))
    //            }
    //          case None =>
    //            ns.+=((x._1, x._2))
    //        }
    //      })
    //
    //      log.debug("[NTOPIC] {}", ns)
    //      ns.par.foreach(
    //        //      subscriberMap.par.foreach(
    //        x => {
    //          x._1 ! PublishMessage(topicName, x._2, message.payload)
    //        }
    //      )
    //
    //      sender ! TopicMessagePublished
    case TopicGetSubscribers =>
      sender ! TopicSubscribers(subscriberMap.toList)
  }
}

case class TopicNode(name: String, topicActor: ActorRef, children: Map[String, TopicNode] = Map[String, TopicNode](), context: ActorRefFactory, root: Boolean = false) {
  def pathToList(path: String): List[String] = {
    path.split("/").toList
  }

  def addNode(path: String): ActorRef = {
    addNode(pathToList(path))
  }

  def addNode(paths: List[String]): ActorRef = {
    paths match {
      case Nil => topicActor
      case x :: Nil => {
        val node: TopicNode = children.get(x) match {
          case Some(node) => node
          case None => {
            val newNodeName = if (root == false) name + "/" + x else x
            val newTopicActor = context.actorOf(Topic.props(newNodeName), Random.alphanumeric.take(128).mkString)
            val node = TopicNode(name = newNodeName, topicActor = newTopicActor, context = context)

            children.+=((x, node))

            node
          }
        }
        node.topicActor
      }
      case x :: others => {
        val node: TopicNode = children.get(x) match {
          case Some(node) => node
          case None => {
            val newNodeName = if (root == false) name + "/" + x else x
            val newTopicActor = context.actorOf(Topic.props(newNodeName), Random.alphanumeric.take(128).mkString)
            val node = TopicNode(name = newNodeName, topicActor = newTopicActor, context = context)
            children.+=((x, node))

            node
          }
        }
        node.addNode(others)
      }
    }
  }

  def removeNode(path: String): Boolean = {
    removeNode(pathToList(path))
  }

  def removeNode(paths: List[String]): Boolean = {
    paths match {
      case Nil => false
      case x :: Nil =>
        children.-(x)
        true
      case x :: others =>
        children.get(x) match {
          case Some(node) => node.removeNode(others)
          case None => false
        }
    }
  }

  def getNodes(path: String): List[ActorRef] = {
    getNodes(pathToList(path))
  }

  def getNodes(paths: List[String]): List[ActorRef] = {
    paths match {
      case Nil => List()
      case x :: Nil => {
        x match {
          case "#" => getEveryNodes()
          case "+" => children.map(x => x._2.topicActor).toList
          case _ => children.get(x) match {
            case Some(node) => List(node.topicActor)
            case None => List()
          }
        }
      }
      case x :: others => {
        x match {
          case "#" => getEveryNodes()
          case "+" => children.map(x => x._2.getNodes(others)).flatten.toList
          case _ => children.get(x) match {
            case Some(node) => node.getNodes(others)
            case None => List()
          }
        }
      }
    }
  }

  def getEveryNodes(): List[ActorRef] = {
    val childrenNodes = children.map(x => {
      x._2.getEveryNodes()
    }).flatten.toList

    // include my topic Actor
    if (!root) topicActor :: childrenNodes else childrenNodes
  }
}

//object Test extends App {
//  val system = ActorSystem()
//  val topicTreeRoot = TopicNode("", null, context = system, root = true)
//
//  topicTreeRoot.addNode("a/b/c")
//  topicTreeRoot.addNode("a/b/c/1")
//  topicTreeRoot.addNode("a/b/c/2")
//  topicTreeRoot.addNode("a")
//
//  println(topicTreeRoot.getNodes("a/b/c/+"))
//
//  println("...")
//
//  println(topicTreeRoot.getNodes("a/#"))
//
//  println("...")
//
//  println(topicTreeRoot.getNodes(""))
//
//  topicTreeRoot.addNode("/a/b")
//
//  println("...")
//
//  println(topicTreeRoot.getNodes("#"))
//
//  println("...")
//
//  println(topicTreeRoot.getNodes("+"))
//}

case class WildcardTopicElement[T](name: String) {

  val subscribers: Set[T] = Set[T]()

  def add(session: T) = {
    subscribers.add(session)
  }

  def remove(session: T) = {
    subscribers.remove(session)
  }
}

case class WildcardTopicNode[T](name: String, elem: Option[WildcardTopicElement[T]] = None, children: Map[String, WildcardTopicNode[T]] = Map[String, WildcardTopicNode[T]](), root: Boolean = false) {
  def pathToList(path: String): List[String] = {
    path.split("/").toList
  }

  def addNode(path: String): Option[WildcardTopicElement[T]] = {
    if (path.contains("+") || path.contains("#"))
      addNode(pathToList(path))
    else
      None
  }

  def addNode(paths: List[String]): Option[WildcardTopicElement[T]] = {
    paths match {
      case Nil => elem
      case x :: Nil => {
        children.get(x) match {
          case Some(node) => node.elem
          case None => {
            val newNodeName = if (!root) name + "/" + x else x
            val newNode = WildcardTopicNode[T](newNodeName, Some(WildcardTopicElement[T](newNodeName)))
            children.+=((x, newNode))
            newNode.elem
          }
        }
      }
      case x :: others => {
        children.get(x) match {
          case Some(node) => node.addNode(others)
          case None => {
            val newNodeName = if (!root) name + "/" + x else x
            val newNode = WildcardTopicNode[T](newNodeName, Some(WildcardTopicElement[T](newNodeName)))
            children.+=((x, newNode))

            newNode.addNode(others)
          }
        }
      }
    }
  }

  def getNode(path: String): Option[WildcardTopicElement[T]] = {
    if (path.contains("+") || path.contains("#"))
      addNode(pathToList(path))
    else
      None
  }

  def matchedElements(path: String): List[Option[WildcardTopicElement[T]]] = {
    matchedElements(pathToList(path))
  }

  def matchedElements(paths: List[String]): List[Option[WildcardTopicElement[T]]] = {
    paths match {
      case Nil => List(elem)
      case x :: Nil => {
        val l1 = children.get("+") match {
          case Some(node) => node :: Nil
          case None => Nil
        }

        val l2 = children.get(x) match {
          case Some(node) => node :: Nil
          case None => Nil
        }

        val l3 = children.get("#") match {
          case Some(node) => node :: Nil
          case None => Nil
        }

        val list = l1 ::: l2 ::: l3
        list.map(l => l.elem)
      }
      case x :: others => {
        val l1 = children.get(x) match {
          case Some(node) => node.matchedElements(others)
          case None => Nil
        }

        val l2 = children.get("+") match {
          case Some(node) => node.matchedElements(others)
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

//object Test2 extends App {
//  val topicRoot = WildcardTopicNode[String]("", root=true)
//  topicRoot.addNode("a/b/+").get.add("c1")
//  topicRoot.addNode("+/+").get.add("c2")
//  topicRoot.addNode("a/#").get.add("c3")
//  topicRoot.addNode("a/b/+").get.add("c4")
//  topicRoot.addNode("a/+/1").get.add("c5")
//  topicRoot.addNode("a/+/1").get.add("c6")
//  topicRoot.addNode("a/+/1").get.add("c7")
//  topicRoot.addNode("#").get.add("c8")
//
//  val a = topicRoot.matchedElements("a/b/c")
//  println(topicRoot.matchedElements("a/b/c"))
//  topicRoot.matchedElements("a/b/c").foreach(x => println(x match {
//    case Some(elem) => elem.subscribers
//  }))
//  println(topicRoot.matchedElements("a/b/1"))
//  topicRoot.matchedElements("a/b/1").foreach(x => println(x match {
//    case Some(elem) => elem.subscribers
//  }))
//  println(topicRoot.matchedElements("x/y"))
//  topicRoot.matchedElements("x/y").foreach(x => println(x match {
//    case Some(elem) => elem.subscribers
//  }))
//
//  println(topicRoot.matchedElements(""))
//  topicRoot.matchedElements("").foreach(x => println(x match {
//    case Some(elem) => elem.subscribers
//  }))
//
//  println(topicRoot.matchedElements("/a"))
//  topicRoot.matchedElements("/a").foreach(x => println(x match {
//    case Some(elem) => elem.subscribers
//  }))
//
////  println(topicRoot.matchedElements("/"))
////  topicRoot.matchedElements("/").foreach(x => println(x match {
////    case Some(elem) => elem.subscribers
////  }))
//
//}
