package plantae.citrus.mqtt.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.io.Tcp.{PeerClosed, Received, Write}
import akka.util.ByteString
import plantae.citrus.mqtt.dto.PacketDecoder
import plantae.citrus.mqtt.dto.connect._
import plantae.citrus.mqtt.dto.ping._
import plantae.citrus.mqtt.dto.publish._
import plantae.citrus.mqtt.dto.subscribe.SUBSCRIBE
import plantae.citrus.mqtt.dto.unsubscribe.UNSUBSCRIBE

/**
 * Created by yinjae on 15. 4. 21..
 */
class PacketBridge extends Actor {
  private val logger = Logging(context.system, this)
  var session: ActorRef = null
  var socket: ActorRef = null

  def receive = {

    case MqttOutboundPacket(packet) => {
      logger.info("relay to TCP ")
      socket ! Write(ByteString(packet.encode))
    }
    case Received(data) => {
      PacketDecoder.decode(data.toArray[Byte]) match {
        case connect: CONNECT => {
          logger.info("receive connect")
          socket = sender
          doSession(connect)
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
      session ! SessionCommand(ConnectionClose)
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
            logger.info("receive session : {}", clientSession)
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

        val returnActor = context.actorOf(Props(new Actor {
          def receive = {
            case session: ActorRef => doSessionActor ! session
              logger.info("clientSession[{}] is passed to [{}]", session.path.name, doSessionActor.path.name)
              context.stop(sessionChecker)
          }
        }))
        ActorContainer.directory.tell(DirectoryReq(clientId), context.actorOf(Props(new Actor {
          override def receive = {
            case DirectoryResp(name, sessionOption) => {
              logger.info("load success DirectoryService")

              sessionOption match {
                case None => {
                  logger.info("not find previous session " + session)
                  createSession(clientId, returnActor)
                }
                case Some(session) =>
                  logger.info("find previous session " + session)
                  session.tell(SessionCommand(SessionPingReq), context.actorOf(Props(new Actor {
                    override def receive = {
                      case SessionPingResp => {
                        logger.info("previous session is valid " + session)
                        if (cleanSession) {
                          session ! SessionCommand(SessionReset)
                        }
                        returnActor ! session
                      }
                      case everythingElse => println(everythingElse)
                    }
                  })))
              }
            }
          }
        })))
      }
    }


    def createSession(clientId: String, returnActor: ActorRef) = {
      logger.info("create new session " + clientId)
      ActorContainer.sessionCreator.tell(clientId,
        context.actorOf(Props(new Actor {
          override def receive = {
            case session: ActorRef => returnActor ! session
          }
        }))
      )
    }
  }

}

