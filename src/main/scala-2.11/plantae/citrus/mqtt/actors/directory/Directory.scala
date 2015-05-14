package plantae.citrus.mqtt.actors.directory

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.cluster._
import plantae.citrus.mqtt.actors._
import plantae.citrus.mqtt.actors.session.{SessionCreateRequest, SessionCreateResponse, SessionExistRequest, SessionExistResponse}

import scala.concurrent.ExecutionContext

sealed trait DirectoryOperation

case class DirectorySessionRequest(name: String) extends DirectoryOperation

case class DirectorySessionResult(actor: ActorRef, isCreated: Boolean) extends DirectoryOperation

class DirectoryProxy extends Actor with ActorLogging {
  implicit val timeout = akka.util.Timeout(2, TimeUnit.SECONDS)
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global


  var directoryCluster: Set[ActorSelection] = Set()
  val cluster = Cluster(context.system)

  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp], classOf[CurrentClusterState]
    , classOf[UnreachableMember], classOf[ReachableMember], classOf[MemberExited], classOf[MemberRemoved])

  override def postStop(): Unit = cluster.unsubscribe(self)


  def receive = {
    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach register

    case MemberRemoved(m, _) => unregister(m)
    case MemberExited(m) => unregister(m)
    case ReachableMember(m) => register(m)
    case UnreachableMember(m) => unregister(m)
    case MemberUp(m) => register(m)

    case request: DirectorySessionRequest =>
      context.actorOf(Props(classOf[ClusterAwareSessionDirectory], sender, directoryCluster)) ! request

    case request: SessionExistRequest =>
      val originalSender = sender
      context.actorOf(Props(new Actor with ActorLogging {
        override def receive = {
          case request: SessionExistRequest => SystemRoot.sessionRoot ! request
          case response: SessionExistResponse => originalSender ! response
            context.stop(self)
        }
      })) ! request

    case request: SessionCreateRequest =>
      val originalSender = sender
      context.actorOf(Props(new Actor with ActorLogging {
        override def receive = {
          case request: SessionCreateRequest => SystemRoot.sessionRoot ! request
          case response: SessionCreateResponse => originalSender ! response
            context.stop(self)
        }
      })) ! request


      // SUBSCRIBE REQUEST ==> SUBSCRIBE RESPONSE
      // UNSUBSCRIBE REQUEST ==> UNSUBSCIRBE RESPONSE
      // PUBLISH REQUEST ==> PUBLISH RESPONSE

  }

  def register(member: Member): Unit =
    directoryCluster = directoryCluster.union(Set(context.actorSelection(RootActorPath(member.address) / "user" / "directory")))

  def unregister(member: Member): Unit =
    directoryCluster = directoryCluster.filter(_ != member)

}

sealed trait ClusterAwareState

case object Scatter extends ClusterAwareState

case object Gather extends ClusterAwareState

case object CreateNew extends ClusterAwareState

sealed trait ClusterAwareData

case class ScatterCount(count: Int) extends ClusterAwareData


class ClusterAwareSessionDirectory(originalSender: ActorRef, cluster: Set[ActorSelection])
  extends FSM[ClusterAwareState, ClusterAwareData] with ActorLogging {

  startWith(Scatter, null)

  when(Scatter) {
    case Event(request: DirectorySessionRequest, _) =>
      cluster.foreach(_ ! SessionExistRequest(request.name))
      goto(Gather) using ScatterCount(cluster.size)
  }

  when(Gather) {
    case Event(SessionExistResponse(sessionId, session), ScatterCount(count)) =>

      session match {
        case Some(x) => originalSender ! DirectorySessionResult(x, false)
          stop(FSM.Shutdown)
        case None =>
          if (count - 1 == 0) {
            cluster.toList(sessionId.hashCode % cluster.size) ! SessionCreateRequest(sessionId)
            goto(CreateNew) using ScatterCount(count - 1)
          } else stay using ScatterCount(count - 1)
      }
  }

  when(CreateNew) {
    case Event(SessionCreateResponse(clientId, newActor), ScatterCount(0)) =>
      originalSender ! DirectorySessionResult(newActor, true)
      stop(FSM.Shutdown)
  }

  initialize()
}
