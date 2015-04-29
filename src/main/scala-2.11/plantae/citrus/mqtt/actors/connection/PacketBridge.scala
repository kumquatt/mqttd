package plantae.citrus.mqtt.actors.connection

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.io.Tcp.{PeerClosed, Received, Write}
import akka.pattern.ask
import akka.util.ByteString
import plantae.citrus.mqtt.actors.SystemRoot
import plantae.citrus.mqtt.actors.directory._
import plantae.citrus.mqtt.actors.session._
import plantae.citrus.mqtt.dto.connect._
import plantae.citrus.mqtt.dto.{Packet, PacketDecoder}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

/**
 * Created by yinjae on 15. 4. 21..
 */


sealed trait PacketBridgeStatus

case object WaitConnect extends PacketBridgeStatus

case object WaitSession extends PacketBridgeStatus

case object WaitConnAct extends PacketBridgeStatus

case object WaitAny extends PacketBridgeStatus

case class ProxyContainer(session: ActorRef, bridge: ActorRef, send: Packet)

class PacketBridge(socket: ActorRef) extends FSM[PacketBridgeStatus, ProxyContainer] with ActorLogging {
  implicit val timeout = akka.util.Timeout(5, TimeUnit.SECONDS)
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  startWith(WaitConnect, ProxyContainer(null, self, null))

  when(WaitConnect) {
    case Event(Received(data), proxyContainer: ProxyContainer) =>
      PacketDecoder.decode(data.toArray) match {
        case (head: CONNECT) :: Nil =>
          val get = Get(head.clientId.value, head.cleanSession)
          val sessionChecker = context.actorOf(Props(classOf[SessionChecker], this))
          sessionChecker.tell(get, self)
          goto(WaitSession) using ProxyContainer(null, proxyContainer.bridge, head)
      }
  }

  when(WaitSession) {
    case Event(session: ActorRef, proxyContainer: ProxyContainer) =>
      session ! MQTTInboundPacket(proxyContainer.send)
      goto(WaitConnAct) using ProxyContainer(session, proxyContainer.bridge, null)
  }




  when(WaitConnAct) {
    case Event(MQTTOutboundPacket(connAck: CONNACK), proxyContainer: ProxyContainer) =>
      socket ! Write(ByteString(connAck.encode))
      goto(WaitAny) using proxyContainer
  }

  when(WaitAny) {
    case Event(MQTTOutboundPacket(packet: Packet), proxyContainer: ProxyContainer) =>
      socket ! Write(ByteString(packet.encode))
      stay() using proxyContainer

    case Event(DISCONNECT, proxyContainer: ProxyContainer) =>
      proxyContainer.session ! MQTTInboundPacket(DISCONNECT)

      stop(FSM.Shutdown)

    case Event(packet: Packet, proxyContainer: ProxyContainer) =>
      proxyContainer.session ! MQTTInboundPacket(packet)
      stay using proxyContainer

    case Event(Received(data), proxyContainer: ProxyContainer) =>
      PacketDecoder.decode(data.toArray).foreach(proxyContainer.bridge ! _)
      stay using proxyContainer

    case Event(PeerClosed, proxyContainer: ProxyContainer) =>
      proxyContainer.session ! ClientCloseConnection
      stop(FSM.Shutdown)
  }

  whenUnhandled {
    case e: Event =>
      log.error("unexpected event : {} ", e)
      stop(FSM.Shutdown)
  }

  initialize()

  case class Get(clientId: String, cleanSession: Boolean)

  class SessionChecker() extends Actor {

    def receive = {
      case Get(clientId, cleanSession) => {
        val doSessionActor: ActorRef = sender()
        SystemRoot.invokeCallback(DirectoryReq(clientId, TypeSession),
          context, Props(new Actor with ActorLogging {
            def receive = {
              case DirectorySessionResult(name, session) => {
                if (cleanSession) {
                  Await.result(session ? SessionReset, Duration.Inf)
                }
                log.debug("clientSession[{}] is passed to [{}]", session.path.name, doSessionActor.path.name)
                doSessionActor ! session
              }
            }
          })
        )
      }
    }
  }

}

