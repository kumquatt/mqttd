package plantae.citrus.mqtt.actors.connection

import java.util.concurrent.TimeUnit

import akka.actor.{ActorLogging, Actor, ActorRef, Props}
import akka.event.Logging
import akka.io.Tcp.{PeerClosed, Received, Write}
import akka.pattern.ask
import akka.util.ByteString
import plantae.citrus.mqtt.actors.ActorContainer
import plantae.citrus.mqtt.actors.directory._
import plantae.citrus.mqtt.actors.session.{MqttOutboundPacket, ClientCloseConnection, MqttInboundPacket, SessionReset}
import plantae.citrus.mqtt.dto.PacketDecoder
import plantae.citrus.mqtt.dto.connect._
import plantae.citrus.mqtt.dto.ping._
import plantae.citrus.mqtt.dto.publish._
import plantae.citrus.mqtt.dto.subscribe.SUBSCRIBE
import plantae.citrus.mqtt.dto.unsubscribe.UNSUBSCRIBE

import scala.concurrent.{ExecutionContext, Await}
import scala.concurrent.duration.Duration


/**
 * Created by yinjae on 15. 4. 21..
 */
class PacketBridge extends Actor with ActorLogging{
  implicit val timeout = akka.util.Timeout(5, TimeUnit.SECONDS)
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global


  private val logger = Logging(context.system, this)
  var session: ActorRef = null
  var socket: ActorRef = null

  def receive = {

    case MqttOutboundPacket(packet) => {
      log.info("out - mqtt header {}" , packet.fixedHeader.packetType.hexa)
      socket ! Write(ByteString(packet.encode))
    }

    case Received(data) => {
      PacketDecoder.decode(data.toArray[Byte]) match {
        case connect: CONNECT => {
          socket = sender
          val bridgeActor = self
          val get = Get(connect.clientId.value, connect.cleanSession)
          context.actorOf(Props(classOf[SessionChecker], this)).tell(get,
            context.actorOf(Props(new Actor {
              def receive = {
                case clientSession: ActorRef =>
                  session = clientSession
                  session.tell(MqttInboundPacket(connect), bridgeActor)
                  context.stop(self)
              }
            })))
        }
        case mqttPacket: PUBLISH => session ! MqttInboundPacket(mqttPacket)
        case mqttPacket: PUBACK => session ! MqttInboundPacket(mqttPacket)
        case mqttPacket: PUBREC => session ! MqttInboundPacket(mqttPacket)
        case mqttPacket: PUBREL => session ! MqttInboundPacket(mqttPacket)
        case mqttPacket: PUBCOMB => session ! MqttInboundPacket(mqttPacket)
        case mqttPacket: SUBSCRIBE => session ! MqttInboundPacket(mqttPacket)
        case mqttPacket: UNSUBSCRIBE => session ! MqttInboundPacket(mqttPacket)
        case PINGREQ => session ! MqttInboundPacket(PINGREQ)
        case DISCONNECT => session ! MqttInboundPacket(DISCONNECT)
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
            session.tell(MqttInboundPacket(connect), bridgeActor)
            context.stop(self)
        }
      })))
  }

  case class Get(clientId: String, cleanSession: Boolean)

  class SessionChecker extends Actor {
    private val logger = Logging(context.system, this)

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
                  logger.info("load success DirectoryService")
                  if (cleanSession) {
                    getOrCreateSession ! SessionReset
                  }
                  logger.info("clientSession[{}] is passed to [{}]", getOrCreateSession.path.name, doSessionActor.path.name)
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

