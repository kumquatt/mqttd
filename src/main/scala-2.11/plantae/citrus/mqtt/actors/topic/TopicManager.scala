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
        (tf.topic, tf.qos, nTopicTreeRoot.getTopicNode(tf.topic))
      })

      // 2. make actor with topic list
      context.actorOf(SubscribeWorker.props(arg)) ! request
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

sealed trait WorkerState

sealed trait SubscribeWorkerData

case object Init extends WorkerState

case object Aggregation extends WorkerState

case class SubscribeAggregationData(count: Int, responses: List[TopicSubscribed], request: Subscribe) extends SubscribeWorkerData

object SubscribeWorker {
  def props(arg: List[(String, Short, ActorRef)]) = {
    Props(classOf[SubscribeWorker], arg)
  }
}

class SubscribeWorker(topics: List[(String, Short, ActorRef)])
  extends FSM[WorkerState, SubscribeWorkerData]
  with ActorLogging {

  startWith(Init, null)

  when(Init) {
    case Event(request: Subscribe, _) =>{
      log.debug("[SubscribeWorker] init topics {} subscribe {} from {}", topics, request, request.session)
      topics.foreach(_ match {
        case (topicName, qos, topic) => {
          topic ! TopicSubscribe(request.session, qos, true)
        }
      })
    }

    goto(Aggregation) using SubscribeAggregationData(0, Nil, request)
  }

  when(Aggregation) {
    case Event(response: TopicSubscribed, a @ SubscribeAggregationData(count, responses, request)) => {
      log.debug("[SubscribeWorker] count {}/{}", count + 1, topics.size)

      // if session is newbie
      if (response.newbie) SystemRoot.topicManager ! PublishRetain(response.topicName, response.session)

      if (topics.size == count + 1){
        // TODO need to change below with using responses
        val result = topics.map(_ => 0.toShort)
        request.session ! Subscribed(result)
        stop(FSM.Shutdown)
      } else
        stay using SubscribeAggregationData(count + 1, response :: responses, request)
    }
  }

  initialize()
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


sealed trait PublishWorkerData

case class PublishAggregationData(count: Int, subscribers: List[(ActorRef, Short)], publish: Publish) extends PublishWorkerData

class PublishWorker(topics: List[ActorRef],  session: ActorRef)
  extends FSM[WorkerState, PublishWorkerData]
  with ActorLogging {

  startWith(Init, null)

  when(Init) {
    case Event(publish: Publish, _) =>
      log.debug("[PublishWorker] init {} {}", publish, topics)
      if (topics.size == 0) session ! Published(publish.packetId, true)
      topics match {
        case x => x.foreach(y => y.tell(TopicGetSubscribers, self))
      }
    goto(Aggregation) using PublishAggregationData(0, Nil, publish)
  }

  when(Aggregation){
    case Event(response: TopicSubscribers, a @ PublishAggregationData(count, subscribers, request)) =>
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
        stay using PublishAggregationData(count+1, subscribers ::: response.subscribers, request)
      }

  }

  initialize()
}
