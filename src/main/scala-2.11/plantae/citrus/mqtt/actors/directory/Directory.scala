package plantae.citrus.mqtt.actors.directory

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}
import akka.cluster._
import akka.pattern.ask
import plantae.citrus.mqtt.actors._
import plantae.citrus.mqtt.actors.session.{SessionCreateRequest, SessionCreateResponse, SessionExistRequest, SessionExistResponse}
import plantae.citrus.mqtt.actors.topic.{TopicCreateRequest, TopicCreateResponse, TopicExistRequest, TopicExistResponse}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

sealed trait DirectoryOperation

case class DirectoryReq(name: String, actorType: ActorType) extends DirectoryOperation

case class DirectorySessionResult(name: String, actor: ActorRef) extends DirectoryOperation

case class DirectoryTopicResult(name: String, actors: List[ActorRef]) extends DirectoryOperation

sealed trait ActorType

case object TypeSession extends ActorType

case object TypeTopic extends ActorType

class DirectoryProxy extends Actor with ActorLogging {
  implicit val timeout = akka.util.Timeout(2, TimeUnit.SECONDS)
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global


  var directoryCluster: Set[ActorSelection] = Set()
  val cluster = Cluster(context.system)

  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach register

    case MemberUp(m) => register(m)

    case request@DirectoryReq(_, TypeSession) =>
      context.actorOf(Props(classOf[ClusterAwareSessionDirectory], sender, directoryCluster)) ! request

    case request@DirectoryReq(_, TypeTopic) =>
      context.actorOf(Props(classOf[ClusterAwareTopicDirectory], sender, directoryCluster)) ! request

    case request: SessionExistRequest =>
      sender ! Await.result(SystemRoot.sessionRoot ? request, Duration.Inf).asInstanceOf[SessionExistResponse]

    case request: SessionCreateRequest =>
      sender ! Await.result(SystemRoot.sessionRoot ? request, Duration.Inf).asInstanceOf[SessionCreateResponse]

    case request: TopicExistRequest =>
      sender ! Await.result(SystemRoot.topicRoot ? request, Duration.Inf).asInstanceOf[TopicExistResponse]

    case request: TopicCreateRequest =>
      sender ! Await.result(SystemRoot.topicRoot ? request, Duration.Inf).asInstanceOf[TopicCreateResponse]
  }

  def register(member: Member): Unit =
    directoryCluster = directoryCluster.union(Set(context.actorSelection(RootActorPath(member.address) / "user" / "directory")))


}

sealed trait ClusterAwareState

case object Scatter extends ClusterAwareState

case object Gather extends ClusterAwareState

case object CreateNew extends ClusterAwareState

sealed trait ClusterAwareData

case class scatterCount(count: Int) extends ClusterAwareData

case object Uninitialize extends ClusterAwareData


class ClusterAwareSessionDirectory(originalSender: ActorRef, cluster: Set[ActorSelection])
  extends FSM[ClusterAwareState, ClusterAwareData] with ActorLogging {

  startWith(Scatter, Uninitialize)
  when(Scatter) {

    case Event(request: DirectoryReq, _) =>
      log.info("cluster aware directory create")
      cluster.foreach(_ ! SessionExistRequest(request.name))
      goto(Gather) using scatterCount(cluster.size)

  }

  when(Gather) {
    case Event(SessionExistResponse(sessionId, session), scatterCount(count)) =>
      log.info("gather directory create")

      session match {
        case Some(x) => originalSender ! DirectorySessionResult(sessionId, x)
          log.info("get session : {}",x)
          stop(FSM.Shutdown)
        case None =>
          log.info("move to directory createNew {}", count)

          if (count - 1 == 0) {
            cluster.toList(sessionId.hashCode % cluster.size) ! SessionCreateRequest(sessionId)
            goto(CreateNew) using scatterCount(count - 1)
          } else stay using scatterCount(count - 1)
      }
  }

  when(CreateNew) {
    case Event(SessionCreateResponse(clientId, newActor), scatterCount(0)) =>
      log.info("move to directory createNew")

      originalSender ! DirectorySessionResult(clientId, newActor)
      stop(FSM.Shutdown)
  }

  initialize()
}

class ClusterAwareTopicDirectory(originalSender: ActorRef, cluster: Set[ActorSelection])
  extends FSM[ClusterAwareState, ClusterAwareData] with ActorLogging {

  startWith(Scatter, Uninitialize)
  when(Scatter) {

    case Event(request: DirectoryReq, _) =>
      log.info("cluster aware directory create")
      cluster.foreach(_ ! TopicExistRequest(request.name))
      goto(Gather) using scatterCount(cluster.size)

  }

  when(Gather) {
    case Event(TopicExistResponse(sessionId, session), scatterCount(count)) =>
      log.info("gather directory create")

      session match {
        case Some(x) => originalSender ! DirectoryTopicResult(sessionId, x)
          stop(FSM.Shutdown)
        case None =>
          log.info("move to directory createNew {}", count)

          if (count - 1 == 0) {
            cluster.toList(sessionId.hashCode % cluster.size) ! TopicCreateRequest(sessionId)
            goto(CreateNew) using scatterCount(count - 1)
          } else stay using scatterCount(count - 1)
      }
  }

  when(CreateNew) {
    case Event(TopicCreateResponse(clientId, newActor), scatterCount(0)) =>
      log.info("move to directory createNew")

      originalSender ! DirectoryTopicResult(clientId, newActor)
      stop(FSM.Shutdown)
  }

  initialize()
}

//class LocalDirectory extends Actor with ActorLogging {
//  implicit val timeout = akka.util.Timeout(2, TimeUnit.SECONDS)
//  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
//
//  def receive = {
//    case DirectoryReq(name, actorType) => {
//      actorType match {
//        case TypeSession =>
//          sender ! DirectorySessionResult(name,
//            Await.result(
//              SystemRoot.sessionRoot ? name, Duration.Inf).asInstanceOf[ActorRef])
//        case TypeTopic =>
//          sender ! DirectoryTopicResult(name,
//            Await.result(
//              SystemRoot.topicRoot ? name, Duration.Inf).asInstanceOf[List[ActorRef]])
//      }
//    }
//  }
//}
