package plantae.citrus.mqtt.actors.topic

import akka.actor.Actor.Receive
import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import plantae.citrus.mqtt.actors.session.{PublishMessage, Session}
import scodec.bits.ByteVector

trait TopicRequest

trait TopicResponse

case class TopicNameQos(topic: String, qos: Short)
case class TopicName(topic: String)

case class Subscribe(topicFilter: List[TopicNameQos], session: ActorRef) extends TopicRequest

case class Subscribed(result: List[Short]) extends TopicResponse

case class Unsubscribe(topicFilter: List[TopicName], session: ActorRef) extends TopicRequest

//case class Unsubscribed extends TopicResponse

case class Publish(topic: String, payload: ByteVector, retain: Boolean, packetId: Option[Int]) extends TopicRequest

case class Published(packetId: Option[Int], result: Boolean) extends TopicResponse

class TopicManager extends Actor with ActorLogging {
  // have topic tree
  val topicTreeRoot = new TopicNode("", null, context = context, root = true)
  val wildcardTopicTreeRoot = new WildcardTopicNode[(ActorRef, Short)]("", root=true)

  def receive = {
    case request: Subscribe => {
      log.debug("[TOPICMANAGER] {}", request)
      // 1. Get Every Topics
      val something = request.topicFilter.map( tf => {
        // 2. store session to widlcardtopic tree
        if (tf.topic.contains("+") || tf.topic.contains("#")) {
          val wildcardNodes = wildcardTopicTreeRoot.getNode(tf.topic) match {
            case None =>
            case Some(elem) => elem.add((request.session, tf.qos))
          }
        } else {
          val sessionNqoss = wildcardTopicTreeRoot.matchedElements(tf.topic).filter(_ != None).map(x => x.get.subscribers.toList).flatten
          val topicNodes = topicTreeRoot.getNodes(tf.topic) match {
            case Nil => {
              List(topicTreeRoot.addNode(tf.topic))
            }
            case nodes => nodes
          }

          for (topicNode <- topicNodes; sessionNqos <- sessionNqoss){
            topicNode ! TopicSubscribe(sessionNqos._1, sessionNqos._2, false)
          }
        }

        val topicNodes: List[ActorRef] = topicTreeRoot.getNodes(tf.topic) match {
          case Nil => {
            // create new node
            if (tf.topic.contains("+") || tf.topic.contains("#")) List()
            else List(topicTreeRoot.addNode(tf.topic))
          }
          case nodes => nodes
        }
        (tf.topic, tf.qos, topicNodes)
      })

      // 2. make actor with topic list
      context.actorOf(SubscirbeWorker.props(something)).tell(request, sender)
    }
    case request: Unsubscribe => {
      // 1. Get Every Topics
      val something = request.topicFilter.map( tf => {
        val topicNodes: List[ActorRef] = topicTreeRoot.getNodes(tf.topic)
        (tf.topic, topicNodes)
      })

      // 2. make actor with topic list
      context.actorOf(UnsubscribeWorker.props(something)).tell(request, sender)
    }

    case request: Publish => {
      // 1. Get Every Topics
      val topics = topicTreeRoot.getNodes(request.topic)
      val topics2 = wildcardTopicTreeRoot.matchedElements(request.topic)

      topics2.filter(x => x != None).foreach(y => y.get.subscribers.foreach(z =>
        z._1 ! PublishMessage(request.topic, z._2, request.payload)
      ))


      // 2. make actor with topic list
      context.actorOf(PublishWorker.props(topics)).tell(request, sender)
    }

//    case request: Publish => {
//
//    }
//      context.actorOf(Props(new Actor {
//        def receive = {
//          case _ =>
//        }
//      })).tell(request, sender)
//    }
  }
}

object SubscirbeWorker {
  def props(something: List[(String, Short, List[ActorRef])]) = {
    Props(classOf[SubscirbeWorker], something)
  }
}

class SubscirbeWorker(something: List[(String, Short, List[ActorRef])]) extends Actor with ActorLogging {
  def receive = {
    // 3. subscribe every topic and send back a result with origin request
    case request: Subscribe =>
      log.debug("[SUBSCRIBEWORKER] {}", request)
      val result = something.map(_ match {
        case(topicName, qos, sessions) => {
          sessions.foreach(s => s ! TopicSubscribe(request.session, qos, false))
          0.toShort
        }}).toList
      log.debug("[SUBSCRIBEWORKER] {}", result)
      sender ! Subscribed(result)
    case _ =>
  }
}

object UnsubscribeWorker {
  def props(something: List[(String, List[ActorRef])]) = {
    Props(classOf[UnsubscribeWorker], something)
  }
}

class UnsubscribeWorker(something: List[(String, List[ActorRef])]) extends Actor with ActorLogging {
  def receive = {
    // 3. unsubscribe every topic and send back a result with origin request
    case request: Unsubscribe =>
    case _ =>
  }
}

object PublishWorker {
  def props(topics: List[ActorRef]) = {
    Props(classOf[PublishWorker], topics)
  }
}

class PublishWorker(topics: List[ActorRef]) extends Actor with ActorLogging {
  def receive = {
    case request: Publish =>
      topics.foreach( x => x ! TopicMessagePublish(request.payload, request.retain, request.packetId))
      sender ! Published(request.packetId, true)
    case _ =>
  }
}
