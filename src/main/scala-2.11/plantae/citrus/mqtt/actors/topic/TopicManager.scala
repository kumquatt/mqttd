package plantae.citrus.mqtt.actors.topic

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import plantae.citrus.mqtt.actors.session.PublishMessage
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
      log.debug("[TopicManager] Publish {}", request)
      // 1. Get Every Topics
      val topics = topicTreeRoot.getNodes(request.topic)
      val topics2 = wildcardTopicTreeRoot.matchedElements(request.topic).flatten.map(x => x.subscribers.toList).flatten

      // 2. make actor with topic list
      context.actorOf(PublishWorker.props(topics, topics2, request, sender))
    }
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
      context.stop(self)
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
      context.stop(self)
    case _ =>
  }
}

object PublishWorker {
  def props(topics: List[ActorRef], wildcardSessions: List[(ActorRef, Short)], request: Publish, session: ActorRef) = {
    Props(classOf[PublishWorker], topics, wildcardSessions, request, session)
  }
}

// FIXME : change to FSM
class PublishWorker(topics: List[ActorRef], wildcardSessions: List[(ActorRef, Short)], request: Publish, session: ActorRef) extends Actor with ActorLogging {

  topics match {
    case Nil => self.tell(TopicSubscribers(List()), self)
    case x => x.foreach(y => y.tell(TopicGetSubscribers, self))
  }
  def receive = {
//    case request: Publish =>
//      log.debug("PublishWorker {} {}", topics, wildcardSessions)
//
//      topics.foreach( x => x ! TopicMessagePublish(request.payload, request.retain, request.packetId, wildcardSessions))
//      sender ! Published(request.packetId, true)
//      context.stop(self)
    case response: TopicSubscribers =>
      val s = response.subscribers.toList ++ wildcardSessions.toList
      log.debug("PublishWorker {} {}", s, request)
      s.par.foreach(
        //      subscriberMap.par.foreach(
        x => {
          x._1 ! PublishMessage(request.topic, x._2, request.payload)
        }
      )
      log.debug("PUBLISHWORKER {}", session)
      session ! Published(request.packetId, true)
      context.stop(self)
    case _ =>
  }
}
