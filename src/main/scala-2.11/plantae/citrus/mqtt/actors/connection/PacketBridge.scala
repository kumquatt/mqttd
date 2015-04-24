package plantae.citrus.mqtt.actors.connection

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp.{PeerClosed, Received, Write}
import akka.pattern.ask
import akka.util.ByteString
import plantae.citrus.mqtt.actors.ActorContainer
import plantae.citrus.mqtt.actors.directory._
import plantae.citrus.mqtt.actors.session.{ClientCloseConnection, MQTTInboundPacket, MQTTOutboundPacket, SessionReset}
import plantae.citrus.mqtt.dto.PacketDecoder
import plantae.citrus.mqtt.dto.connect._
import plantae.citrus.mqtt.dto.ping._
import plantae.citrus.mqtt.dto.publish._
import plantae.citrus.mqtt.dto.subscribe.SUBSCRIBE
import plantae.citrus.mqtt.dto.unsubscribe.UNSUBSCRIBE

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}


/**
 * Created by yinjae on 15. 4. 21..
 */
class PacketBridge(socket: ActorRef) extends Actor with ActorLogging {
  implicit val timeout = akka.util.Timeout(5, TimeUnit.SECONDS)
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  var session: ActorRef = null
  val bridge = self

  def receive = {

    case MQTTOutboundPacket(packet) => {
      socket ! Write(ByteString(packet.encode))
    }

    case Received(data) => {
      PacketDecoder.decode(data.toArray[Byte]) match {
        case connect: CONNECT => {
          val get = Get(connect.clientId.value, connect.cleanSession)
          context.actorOf(Props(classOf[SessionChecker], this)).tell(get,
            context.actorOf(Props(new Actor {
              def receive = {
                case clientSession: ActorRef =>
                  session = clientSession
                  session.tell(MQTTInboundPacket(connect), bridge)
                  context.stop(self)
              }
            })))
        }
        case mqttPacket: PUBLISH => session ! MQTTInboundPacket(mqttPacket)
        case mqttPacket: PUBACK => session ! MQTTInboundPacket(mqttPacket)
        case mqttPacket: PUBREC => session ! MQTTInboundPacket(mqttPacket)
        case mqttPacket: PUBREL => session ! MQTTInboundPacket(mqttPacket)
        case mqttPacket: PUBCOMB => session ! MQTTInboundPacket(mqttPacket)
        case mqttPacket: SUBSCRIBE => session ! MQTTInboundPacket(mqttPacket)
        case mqttPacket: UNSUBSCRIBE => session ! MQTTInboundPacket(mqttPacket)
        case PINGREQ => session ! MQTTInboundPacket(PINGREQ)
        case DISCONNECT => session ! MQTTInboundPacket(DISCONNECT)
      }
    }
    case PeerClosed => {
      session ! ClientCloseConnection
      context.stop(self)
    }

  }

  def doSession(connect: CONNECT) = {
    val bridgeActor = self
    context.actorOf(Props(classOf[SessionChecker], this)).tell(Get(connect.clientId.value, connect.cleanSession),
      context.actorOf(Props(new Actor {
        def receive = {
          case clientSession: ActorRef =>
            session = clientSession
            session.tell(MQTTInboundPacket(connect), bridgeActor)
            context.stop(self)
        }
      })))
  }

  case class Get(clientId: String, cleanSession: Boolean)

  class SessionChecker extends Actor {
    override def preStart = {
      super.preStart
      log.debug("create session checker {}", self.path.name)
    }

    override def postStop = {
      super.postStop
      log.debug("remove session checker {}", self.path.name)
    }


    def receive = {
      case Get(clientId, cleanSession) => {
        val sessionChecker = self
        val doSessionActor = sender

        val directoryProxyActor = ActorContainer.directoryProxyMaster
        val future = Await.result(directoryProxyActor ? GetDirectoryActor, Duration.Inf)
        future match {
          case a: DirectoryActor =>
            a.actor.tell(DirectoryReq(clientId, TypeSession), context.actorOf(Props(new Actor {
              override def receive = {
                case DirectoryResp(name, getOrCreateSession) => {
                  log.info("load success DirectoryService")
                  if (cleanSession) {
                    getOrCreateSession ! SessionReset
                  }
                  log.info("clientSession[{}] is passed to [{}]", getOrCreateSession.path.name, doSessionActor.path.name)
                  doSessionActor ! getOrCreateSession
                  context.stop(sessionChecker)
                }
              }
            })))
        }
      }
    }
  }

}

