package plantae.citrus.mqtt.actors.connection

import akka.actor._
import akka.io.Tcp.{PeerClosed, Received, Write}
import akka.util.ByteString
import plantae.citrus.mqtt.actors.SystemRoot
import plantae.citrus.mqtt.actors.directory._
import plantae.citrus.mqtt.actors.session._
import plantae.citrus.mqtt.dto.connect._
import plantae.citrus.mqtt.packet.{ConnAckPacket, ConnectPacket, ControlPacket, DisconnectPacket}
import scodec.Codec
import scodec.bits.BitVector

sealed trait BridgeState

case object ConnectionWaitConnect extends BridgeState

case object ConnectionGetSession extends BridgeState

case object ConnectionForwardConnAct extends BridgeState

case object ConnectionEstablished extends BridgeState

sealed trait BridgeData

case class Session(actor: ActorRef, isCreated: Boolean)

case class SessionCreateContainer(bridge: ActorRef, toSend: ControlPacket) extends BridgeData

case class BridgeContainer(session: ActorRef, bridge: ActorRef, isCreated: Boolean) extends BridgeData

case class RestByteContainer(session: ActorRef, bridge: ActorRef, remainingBytes: BitVector) extends BridgeData

class PacketBridge(socket: ActorRef) extends FSM[BridgeState, BridgeData] with ActorLogging {
  startWith(ConnectionWaitConnect, SessionCreateContainer(self, null))

  when(ConnectionWaitConnect) {
    case Event(Received(data), container: SessionCreateContainer) =>
      PacketDecoder.decode(BitVector(data)) match {
        case ((head: ConnectPacket) :: Nil, _) =>
          SystemRoot.directoryProxy.tell(DirectorySessionRequest(head.clientId),
            context.actorOf(Props(new Actor with ActorLogging {
              def receive = {
                case DirectorySessionResult(session, isCreated) => {
                  if (head.variableHeader.cleanSession && !isCreated) {
                    context.watch(session)
                    context.stop(session)
                    context.become({
                      case Terminated(x) =>
                        SystemRoot.sessionRoot ! session.path.name
                      case newSession: ActorRef =>
                        container.bridge ! Session(newSession, true)
                        context.stop(self)
                    })
                  } else {
                    container.bridge ! Session(session, isCreated)
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
    case Event(session: Session, container: SessionCreateContainer) =>
      session.actor ! MQTTInboundPacket(container.toSend)
      goto(ConnectionForwardConnAct) using BridgeContainer(session.actor, container.bridge, session.isCreated)
  }


  when(ConnectionForwardConnAct) {
    case Event(MQTTOutboundPacket(connAck: ConnAckPacket), container: BridgeContainer) =>
      socket ! Write(ByteString(Codec[ControlPacket].encode(ConnAckPacket(connAck.fixedHeader, container.isCreated, connAck.returnCode)).require.toByteBuffer))
      goto(ConnectionEstablished) using new RestByteContainer(container.session, container.bridge, BitVector.empty)
  }

  when(ConnectionEstablished) {
    case Event(MQTTOutboundPacket(packet: ControlPacket), container: RestByteContainer) =>
      socket ! Write(ByteString(Codec[ControlPacket].encode(packet).require.toByteBuffer))
      stay using container

    case Event(disconnect: DisconnectPacket, container: RestByteContainer) =>
      container.session ! MQTTInboundPacket(DisconnectPacket())
      stop(FSM.Shutdown)

    case Event(packet: ControlPacket, container: RestByteContainer) =>
      container.session ! MQTTInboundPacket(packet)
      stay using container

    case Event(Received(data), container: RestByteContainer) =>
      val decodeResult = PacketDecoder.decode((container.remainingBytes ++ BitVector(data)))
      decodeResult._1.foreach(self ! _)
      stay using new RestByteContainer(container.session, container.bridge, decodeResult._2)

    case Event(PeerClosed, container: RestByteContainer) =>
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

