package plantae.citrus.mqtt.actors.topic

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import plantae.citrus.mqtt.actors.SystemRoot
import plantae.citrus.mqtt.actors.session.PublishMessage
import plantae.citrus.mqtt.actors.topic.TopicPublishRetainMessage
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

case class PublishRetain(topic: String, session: ActorRef)

class TopicManager extends Actor with ActorLogging {
  // have topic tree
  val nTopicTreeRoot = new TopicNode2("", null, context, true)

  def receive = {
    case request: Subscribe => {
      log.debug("[TOPICMANAGER] {}", request)
      // 1. Get Every Topics
      val arg = request.topicFilter.map( tf => {
        log.debug("TOPICMANAGER!!!! {}", nTopicTreeRoot.matchedTopicNodes("#"))
        (tf.topic, tf.qos, nTopicTreeRoot.getTopicNode(tf.topic))
      })

      // 2. make actor with topic list
      context.actorOf(SubscirbeWorker.props(arg)).tell(request, sender)
    }
    case request: Unsubscribe => {
//      // 1. Get Every Topics
//      val something = request.topicFilter.map( tf => {
//        val topicNodes: List[ActorRef] = nTopicTreeRoot.getTopicNode(tf.topic)
//        (tf.topic, topicNodes)
//      })
//
//      // 2. make actor with topic list
//      context.actorOf(UnsubscribeWorker.props(something)).tell(request, sender)
    }

    case request: Publish => {
      log.debug("[TopicManager] Publish {}", request)
      // 1. Get Every Topics
      if (request.retain == true){
        log.debug("[TopicManager] Topic({}) Store retained message {} ", request.topic, request)
        nTopicTreeRoot.getTopicNode(request.topic) ! TopicStoreRetainMessage(request.payload)
      }

      val topics = nTopicTreeRoot.matchedTopicNodesWithOutWildCard(request.topic)

      // 2. make actor with topic list
      context.actorOf(PublishWorker.props(topics, request, sender))
    }

    case publish: PublishRetain => {

      val topics = nTopicTreeRoot.matchedTopicNodes(publish.topic)
      log.debug("PublishRetain {} {}", topics, publish)
      topics.foreach( x => x ! TopicPublishRetainMessage(publish.session))
    }
  }
}

object SubscirbeWorker {
  def props(arg: List[(String, Short, ActorRef)]) = {
    Props(classOf[SubscirbeWorker], arg)
  }
}

class SubscirbeWorker(something: List[(String, Short, ActorRef)]) extends Actor with ActorLogging {
  var count = 0

  def receive = {
    // 3. subscribe every topic and send back a result with origin request
    case request: Subscribe =>
      log.debug("[SUBSCRIBEWORKER] {}", request)
      val result = something.map(_ match {
        case(topicName, qos, topic) => {
          topic ! TopicSubscribe(request.session, qos, true)
          0.toShort
        }}).toList
      log.debug("[SUBSCRIBEWORKER] {}", result)

    case response: TopicSubscribed => {
      count = count + 1
      log.debug("[SUBSCRIBEWORKER] {} {}/{}", response, count, something.size)

      if (response.newbie) SystemRoot.topicManager ! PublishRetain(response.topicName, response.session)

      if (something.size == count) {
        val result = something.map(x => 0.toShort)
        sender ! Subscribed(result)
        context.stop(self)
      }
    }
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
  def props(topics: List[ActorRef], request: Publish, session: ActorRef) = {
    Props(classOf[PublishWorker], topics, request, session)
  }
}

// FIXME : change to FSM
class PublishWorker(topics: List[ActorRef], request: Publish, session: ActorRef) extends Actor with ActorLogging {

  var count = 0
  var subscribers = scala.collection.mutable.Set[(ActorRef, Short)]()
  topics match {
    case x => x.foreach(y => y.tell(TopicGetSubscribers, self))
  }

  def receive = {
    case response: TopicSubscribers =>
      count = count + 1

      subscribers = subscribers.++(response.subscribers)

      log.debug("PUBLISHWORKER {} {}/{}", subscribers, count, topics.size)

      if (topics.size == count) {
        log.debug("PublishWorker {}", subscribers)
        subscribers.par.foreach(
        x => {
          x._1 ! PublishMessage(request.topic, x._2, request.payload)
        }
        )
        session ! Published(request.packetId, true)
        context.stop(self)
      }
    case _ =>
  }
}
