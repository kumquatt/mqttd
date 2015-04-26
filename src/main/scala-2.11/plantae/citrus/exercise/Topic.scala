//package plantae.citrus.exercise
//
//import akka.actor._
//import com.typesafe.config.ConfigFactory
//
//import scala.collection.mutable.Map
//
//sealed trait TopicRequest
//
//sealed trait TopicResponse
//
//case class Subscribe(clientId: String) extends TopicRequest
//
//case class Unsubscribe(clientId: String) extends TopicRequest
//
//case object ClearList extends TopicRequest
//
//case class TopicInMessage(payload: Array[Byte], qos: Short, retain: Boolean, packetId: Option[Int]) extends TopicRequest
//
//case object TopicInMessageAck extends TopicResponse
//
//case class TopicOutMessage(payload: Array[Byte], qos: Short, retain: Boolean, topic: String) extends TopicResponse
//
//case object TopicOutMessageAck extends TopicRequest
//
//class Topic(path: String) extends Actor with ActorLogging {
//
//  val subscribers: Map[String, ActorRef] = Map()
//
//  def receive = {
//    case Subscribe(clientId) => {
//      log.info("Subscribe client({}) topic({})", clientId, self.path.name)
//      subscribers.+=((clientId, sender))
//      printEverySubscriber
//    }
//
//    case Unsubscribe(clientId) => {
//      log.debug("Unsubscribe client({}) topic({})", clientId, self.path.name)
//      subscribers.-(clientId)
//      printEverySubscriber
//    }
//
//    case ClearList => {
//      log.debug("Clear subscriber list")
//      subscribers.clear()
//      printEverySubscriber
//    }
//
//    case TopicInMessage(payload, qos, retain, packetId) => {
//      log.debug("qos : {} , retain : {} , payload : {} , sender {}", qos, retain, new String(payload), sender)
//      sender ! TopicInMessageAck
//      subscribers.values.foreach(
//        (actor) => actor ! TopicOutMessage(payload, qos, retain, self.path.name)
//      )
//    }
//
//    case TopicOutMessageAck =>
//
//  }
//
//
//  def printEverySubscriber = {
//    log.info("{}'s subscriber ", self.path.name)
//    subscribers.foreach(s => log.info("{},", s._1))
//  }
//
//}
//
//class TempTopic(name: String) {
//  def printName = {
//    println(name)
//  }
//}
//
//object Test extends App {
//  val r = RootTopicNode
//  r.addNode("a/d/g", new TempTopic("a/d/g"))
//  r.addNode("a/d/h", new TempTopic("a/d/h"))
//
//  printList(r.getNodes("a/d/g"))
//  printList(r.getNodes("a/d/h"))
//
//  println(r.getNodes("a/+/"))
//
//  def printList(list: List[TempTopic]): Unit ={
//    list.foreach(tt => tt.printName)
//  }
//}
//
//
//object RootTopicNode {
//  val children: Map[String, TopicNode] = Map()
//  var topic: TempTopic = null
//
//  def addNode(path: String, topic: TempTopic) = {
//    val paths = pathToList(path)
//    if (paths.size == 0)
//      this.topic = topic
//    else {
//      children.get(paths.head) match {
//        case Some(x: TopicNode) =>
//          x.addNode(paths.tail, topic)
//        case None => {
//          val childNode = new TopicNode(paths.head)
//          childNode.addNode(paths.tail, topic)
//          children.+=((paths.head, childNode))
//        }
//      }
//    }
//  }
//
//  def removeNode(path: String) = {
//    val paths = pathToList(path)
//
//    if (paths.size == 1) {
//      children.remove(paths.head)
//    } else {
//      children.get(paths.head) match {
//        case Some(x: TopicNode) =>
//          x.removeNode(paths.tail)
//      }
//    }
//  }
//
//  def getNodes(path: String): List[TempTopic] = {
//
//    val paths = pathToList(path)
//
//    children.get(paths.head) match {
//      case Some(x: TopicNode) =>
//        x.getNodes(paths.tail, Nil)
//      case None =>
//        Nil
//    }
//  }
//
//  def pathToList(path: String): List[String] = {
//    path.split("/").toList
//  }
//}
//
//class TopicNode(name: String, children: Map[String, TopicNode] = Map()) {
//  var topic: TempTopic = null
//
//  def addNode(paths: List[String], topic: TempTopic): Boolean = {
//    if (paths != Nil) {
//      children.get(paths.head) match {
//        case Some(x: TopicNode) =>
//          x.addNode(paths.tail, topic)
//        case None =>
//          val childNode = new TopicNode(paths.head)
//          childNode.addNode(paths.tail, topic)
//          children.+=((paths.head, childNode))
//      }
//    } else {
//      this.topic = topic
//    }
//
//    true
//  }
//
//  def removeNode(paths: List[String]): Boolean = {
//    if (paths != Nil) {
//      if (paths.size == 1) {
//        children.remove(paths.head)
//      } else {
//        children.get(paths.head) match {
//          case Some(x: TopicNode) =>
//            x.removeNode(paths.tail)
//        }
//      }
//
//    }
//    true
//  }
//
//  def getNodes(paths: List[String], acc: List[TempTopic]): List[TempTopic] = {
//    if (paths == Nil) {
//      topic :: acc
//    } else {
//      if ("+".eq(paths.head)){
//        children.map(x => {
//          x._2.getNodes(paths.tail,Nil)
//        }).toList :: acc
//
//      } else {
//        children.get(paths.head) match {
//          case Some(x: TopicNode) =>
//            x.getNodes(paths.tail, acc)
//          case None =>
//            acc
//        }
//      }
//    }
//  }
//}
//
