package plantae.citrus.mqtt.actors.topic

import akka.actor._
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

case class Unsubscribed(result: List[Short]) extends TopicResponse

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
      context.actorOf(SubscirbeWorker.props(arg, sender)).tell(request, sender)
    }
    case request: Unsubscribe => {
      val something = request.topicFilter.map( tf => {
        (tf.topic, nTopicTreeRoot.getTopicNode(tf.topic))
      })

      context.actorOf(UnsubscribeWorker.props(something, sender)).tell(request, sender)
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
      context.actorOf(PublishWorker.props(topics, sender)) ! request
    }

    case publish: PublishRetain => {

      val topics = nTopicTreeRoot.matchedTopicNodes(publish.topic)
      log.debug("PublishRetain {} {}", topics, publish)
      topics.foreach( x => x ! TopicPublishRetainMessage(publish.session))
    }
  }
}

object SubscirbeWorker {
  def props(arg: List[(String, Short, ActorRef)], originSession: ActorRef) = {
    Props(classOf[SubscirbeWorker], arg, originSession)
  }
}

class SubscirbeWorker(something: List[(String, Short, ActorRef)], originSession: ActorRef) extends Actor with ActorLogging {
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
        log.debug("[SUBSCRIBEWORKER] i will send back subscribed to session({}) topic({})", sender, response.topicName)
        originSession ! Subscribed(result)
        context.stop(self)
      }
    }
    case _ =>

  }
}

object UnsubscribeWorker {
  def props(something: List[(String, ActorRef)], originSession: ActorRef) = {
    Props(classOf[UnsubscribeWorker], something, originSession)
  }
}

class UnsubscribeWorker(something: List[(String, ActorRef)], originSession: ActorRef) extends Actor with ActorLogging {
  def receive = {
    // 3. unsubscribe every topic and send back a result with origin request
    case request: Unsubscribe =>
      val result = something.map(x => {
        x._2 ! TopicUnsubscribe(request.session)
        0.toShort
      })

      log.debug("UnsubscribeWorker {} {}", something, originSession)
      originSession ! Unsubscribed(result)
      context.stop(self)
    case _ =>
  }
}

object PublishWorker {
  def props(topics: List[ActorRef], session: ActorRef) = {
    Props(classOf[PublishWorker], topics, session)
  }
}

sealed trait PublishWorkerState

sealed trait PublishWorkerData

case object Init extends PublishWorkerState

case object Aggregation extends PublishWorkerState

case class PublishRequest(publish: Publish) extends PublishWorkerData
case class AggregationData(count: Int, subscribers: List[(ActorRef, Short)], publish: Publish) extends PublishWorkerData

class PublishWorker(topics: List[ActorRef],  session: ActorRef)
  extends FSM[PublishWorkerState, PublishWorkerData]
  with ActorLogging {

  startWith(Init, null)

  when(Init) {
    case Event(publish: Publish, _) =>
      log.debug("[PublishWorker] init {}", publish)
      topics match {
        case x => x.foreach(y => y.tell(TopicGetSubscribers, self))
      }
    goto(Aggregation) using PublishRequest(publish)
  }

  when(Aggregation){
    case Event(response: TopicSubscribers, p @ PublishRequest(request)) =>
      log.debug("[PublishWorker] Aggregation {} {}", response, p)
      if (topics.size == 1){

        log.debug("PublishWorker {} " + request.topic, response.subscribers)
        response.subscribers.par.foreach(
          x => {
            x._1 ! PublishMessage(request.topic, x._2, request.payload)
          }
        )
        //fixme : change the name
//        log.debug("PublishWorker .. session({})", session)
        session ! Published(request.packetId, true)
        stop(FSM.Shutdown)
      }else {
        stay using AggregationData(1, response.subscribers, request)
      }
    case Event(response: TopicSubscribers, a @ AggregationData(count, subscribers, request)) =>
      log.debug("PublishWorker {} {}/{} " + request.topic, subscribers, count + 1, topics.size)

      if (topics.size == count + 1){

        log.debug("PublishWorker {}", subscribers)
        (subscribers ::: response.subscribers).par.foreach(
          x => {
            log.debug("PublishWorker publish to({}) request({})", x._1, request.topic)
            x._1 ! PublishMessage(request.topic, x._2, request.payload)
          }
        )
        session ! Published(request.packetId, true)
        stop(FSM.Shutdown)
      }else {
        stay using AggregationData(count+1, subscribers ::: response.subscribers, request)
      }

  }

  initialize()
}
