package plantae.citrus.mqtt.actors.topic

import akka.actor._

import scala.collection.mutable.Map
import scala.util.Random

sealed trait TopicRequest

sealed trait TopicResponse

case class Subscribe(clientId: String) extends TopicRequest

case class Unsubscribe(clientId: String) extends TopicRequest

case object ClearList extends TopicRequest

case class TopicInMessage(payload: Array[Byte], qos: Short, retain: Boolean, packetId: Option[Short]) extends TopicRequest

case object TopicInMessageAck extends TopicResponse

case class TopicOutMessage(payload: Array[Byte], qos: Short, retain: Boolean, topic: String) extends TopicResponse

case object TopicOutMessageAck extends TopicRequest

class TopicRoot extends Actor with ActorLogging {

  val root = DiskTreeNode[ActorRef]("", "", Map[String, DiskTreeNode[ActorRef]]())

  override def receive = {
    case topicName: String => {
      root.getNodes(topicName) match {
        case Nil => {
          log.debug("new topic is created[{}]", topicName)
          val topic = context.actorOf(Props(classOf[Topic], topicName), Random.alphanumeric.take(128).mkString)
          root.addNode(topicName, topic)
          sender ! List(topic)
        }
        case topics: List[ActorRef] => sender ! topics

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
      log.debug("Subscribe client({}) topic({})", clientId, name)
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
        (actor) => {
          actor ! TopicOutMessage(payload, qos, retain, name)}
      )
    }
    case TopicOutMessageAck =>
  }

  def printEverySubscriber = {
    log.debug("{}'s subscriber ", name)
    subscriberMap.foreach(s => log.debug("{},", s._1))
  }
}

case class DiskTreeNode[A](name: String, fullPath: String, children: Map[String, DiskTreeNode[A]] = Map[String, DiskTreeNode[A]]()){
  var topic: Option[A] = None

  def pathToList(path: String) : List[String] = {
    path.split("/").toList
  }

  def addNode(path: String, topic: A): Boolean = {
    addNode(pathToList(path), path, topic)
  }

  def addNode(paths: List[String], path: String, topic: A): Boolean = {
    paths match {
      case Nil => this.topic = Some(topic)
      case _ => {
        children.get(paths.head) match {
          case Some(node: DiskTreeNode[A]) => {
            node.addNode(paths.tail, path, topic)
          }
          case None => {
            val node = new DiskTreeNode[A](paths.head, fullPath +"/" +paths.head)
            node.addNode(paths.tail, path, topic)
            children.+=((paths.head, node))
          }
        }
      }
    }

    true
  }

  def removeNode(path: String): Boolean = {
    removeNode(pathToList(path))
  }

  def removeNode(paths: List[String]): Boolean = {
    if(paths.size == 1){
      children.-(paths.head)
    } else if(paths.size > 1) {
      children.get(paths.head) match {
        case Some(node: DiskTreeNode[A]) => {
          node.removeNode(paths.tail)
        }
        case None =>
      }
    }

    true
  }

  def getNodes(path: String): List[A] = {
    getNodes(pathToList(path))
  }

  def getNodes(paths: List[String]) : List[A] = {
    paths match {
      case Nil => List()
      case x :: Nil => {
        x match {
          case "*" => getEveryNodes()
          case "+" => {
            children.filter(x => x._2.topic.isDefined).map(y => y._2.topic.get).toList
          }
          case _ => {
            children.get(x) match {
              case Some(node:DiskTreeNode[A]) => {
                node.topic match {
                  case Some(t) => List(t)
                  case None => List()
                }

              }
              case None => List()
            }
          }
        }
      }
      case x :: others => {
        x match {
          case "+" => {
            children.map(x => {
              x._2.getNodes(others)
            }).flatten.toList
          }
          case _ => {
            children.get(x) match {
              case Some(node: DiskTreeNode[A]) => node.getNodes(others)
              case None => List()
            }
          }
        }
      }
    }
  }

  def getEveryNodes(): List[A] = {
    val topics = children.map(x => {
      x._2.getEveryNodes()
    }).flatten.toList

    topic match {
      case Some(x) => x :: topics
      case None => topics
    }
  }
}
