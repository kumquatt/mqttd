package plantae.citrus.mqtt.actors.connection

import akka.actor._
import akka.io.Tcp.{PeerClosed, Received, Write}
import akka.util.ByteString
import plantae.citrus.mqtt.actors.SystemRoot
import plantae.citrus.mqtt.actors.directory._
import plantae.citrus.mqtt.actors.session._
import plantae.citrus.mqtt.dto.connect._
import plantae.citrus.mqtt.dto.{Packet, PacketDecoder}

sealed trait PacketBridgeStatus

case object WaitConnect extends PacketBridgeStatus

case object WaitSession extends PacketBridgeStatus

case object WaitConnAct extends PacketBridgeStatus

case object WaitAny extends PacketBridgeStatus

sealed trait ProxyData

case class SessionCreateContainer(bridge: ActorRef, send: Packet) extends ProxyData

case class BridgeContainer(session: ActorRef, bridge: ActorRef) extends ProxyData

class PacketBridge(socket: ActorRef) extends FSM[PacketBridgeStatus, ProxyData] with ActorLogging {
  startWith(WaitConnect, SessionCreateContainer(self, null))

  when(WaitConnect) {
    case Event(Received(data), container: SessionCreateContainer) =>
      PacketDecoder.decode(data.toArray) match {
        case (head: CONNECT) :: Nil =>
          SystemRoot.invokeCallback(DirectoryReq(head.clientId.value, TypeSession),
            context, Props(new Actor with ActorLogging {
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
          goto(WaitSession) using SessionCreateContainer(container.bridge, head)
      }
  }

  when(WaitSession) {
    case Event(session: ActorRef, container: SessionCreateContainer) =>
      session ! MQTTInboundPacket(container.send)
      goto(WaitConnAct) using BridgeContainer(session, container.bridge)
  }


  when(WaitConnAct) {
    case Event(MQTTOutboundPacket(connAck: CONNACK), container: BridgeContainer) =>
      socket ! Write(ByteString(connAck.encode))
      goto(WaitAny) using container
  }

  when(WaitAny) {
    case Event(MQTTOutboundPacket(packet: Packet), container: BridgeContainer) =>
      socket ! Write(ByteString(packet.encode))
      stay() using container

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

