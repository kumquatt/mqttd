package plantae.citrus.mqtt.actors.connection

import akka.actor._
import akka.io.Tcp.{PeerClosed, Received, Write}
import akka.util.ByteString
import plantae.citrus.mqtt.actors.SystemRoot
import plantae.citrus.mqtt.actors.directory._
import plantae.citrus.mqtt.actors.session._
import plantae.citrus.mqtt.dto.connect._
import plantae.citrus.mqtt.dto.{Packet, PacketDecoder}

sealed trait BridgeState

case object ConnectionWaitConnect extends BridgeState

case object ConnectionGetSession extends BridgeState

case object ConnectionForwardConnAct extends BridgeState

case object ConnectionEstablished extends BridgeState

sealed trait BridgeData

case class SessionCreateContainer(bridge: ActorRef, send: Packet) extends BridgeData

case class BridgeContainer(session: ActorRef, bridge: ActorRef) extends BridgeData

class PacketBridge(socket: ActorRef) extends FSM[BridgeState, BridgeData] with ActorLogging {
  startWith(ConnectionWaitConnect, SessionCreateContainer(self, null))

  when(ConnectionWaitConnect) {
    case Event(Received(data), container: SessionCreateContainer) =>
      PacketDecoder.decode(data.toArray) match {
        case (head: CONNECT) :: Nil =>
          SystemRoot.directoryProxy.tell(DirectoryReq(head.clientId.value, TypeSession),
            context.actorOf(Props(new Actor with ActorLogging {
              def receive = {
                case DirectorySessionResult(name, session) => {
                  if (head.cleanSession) {
                    context.watch(session)
                    context.stop(session)
                    context.become({ case Terminated(x) =>
                      SystemRoot.sessionRoot ! name
                    case newSession: ActorRef =>
                      container.bridge ! newSession
                      context.stop(self)
                    })
                  } else {
                    container.bridge ! session
                    context.stop(self)
                  }
                }
              }
            })
            )
          )
          goto(ConnectionGetSession) using SessionCreateContainer(container.bridge, head)
        case _ => stop(FSM.Shutdown)
      }
  }

  when(ConnectionGetSession) {
    case Event(session: ActorRef, container: SessionCreateContainer) =>
      session ! MQTTInboundPacket(container.send)
      goto(ConnectionForwardConnAct) using BridgeContainer(session, container.bridge)
  }


  when(ConnectionForwardConnAct) {
    case Event(MQTTOutboundPacket(connAck: CONNACK), container: BridgeContainer) =>
      socket ! Write(ByteString(connAck.encode))
      goto(ConnectionEstablished) using container
  }

  when(ConnectionEstablished) {
    case Event(MQTTOutboundPacket(packet: Packet), container: BridgeContainer) =>
      socket ! Write(ByteString(packet.encode))
      stay using container

    case Event(DISCONNECT, container: BridgeContainer) =>
      container.session ! MQTTInboundPacket(DISCONNECT)
      stop(FSM.Shutdown)

    case Event(packet: Packet, container: BridgeContainer) =>
      container.session ! MQTTInboundPacket(packet)
      stay using container

    case Event(Received(data), container: BridgeContainer) =>
      PacketDecoder.decode(data.toArray).foreach(container.bridge ! _)
      stay using container

    case Event(PeerClosed, container: BridgeContainer) =>
      container.session ! ClientCloseConnection
      stop(FSM.Shutdown)
  }

  whenUnhandled {
    case e: Event =>
      log.error("unexpected event : {} ", e)
      stop(FSM.Shutdown)
  }

  initialize()
}

