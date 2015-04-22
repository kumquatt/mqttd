package plantae.citrus.mqtt.actors


import java.util.concurrent.TimeUnit

import akka.actor._
import akka.event.Logging
import akka.pattern.ask
import plantae.citrus.mqtt.dto.connect.{CONNACK, CONNECT, DISCONNECT, ReturnCode, Will}
import plantae.citrus.mqtt.dto.ping.{PINGREQ, PINGRESP}
import plantae.citrus.mqtt.dto.publish.PUBLISH
import plantae.citrus.mqtt.dto.subscribe.{SUBACK, SUBSCRIBE, TopicFilter}
import plantae.citrus.mqtt.dto.unsubscribe.UNSUBSCRIBE
import plantae.citrus.mqtt.dto.{BYTE, Packet, STRING}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}


case class MqttInboundPacket(mqttPacket: Packet)

case class MqttOutboundPacket(mqttPacket: Packet)

class SessionCommand

case object SessionReset extends SessionCommand

case object SessionResetAck extends SessionCommand

case object SessionKeepAliveTimeOut extends SessionCommand

case object ClientCloseConnection extends SessionCommand

class SessionCreator extends Actor {
  override def receive = {
    case clientId: String => sender ! context.actorOf(Props[Session], clientId)
  }
}

class Session extends DirectoryMonitorActor {
  implicit val timeout = akka.util.Timeout(5, TimeUnit.SECONDS)
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private val logger = Logging(context.system, this)

  case class ConnectionStatus(will: Option[Will], keepAliveTime: Int)

  var connectionStatus: Option[ConnectionStatus] = None
  var keepAliveTimer: Option[Cancellable] = None

  override def actorType: ActorType = SESSION

  override def receive: Receive = {
    case MqttInboundPacket(packet) => mqttPacket(packet, sender)
    case command: SessionCommand => session(command)
    case RegisterAck(name) => {
      logger.info("receive register ack")
    }
    case everythingElse => println(everythingElse)
  }


  def session(command: SessionCommand): Unit = command match {

    case SessionReset => {
      logger.info("session reset : " + self.toString())
      connectionStatus = None
      sender ! SessionResetAck
    }

    case SessionKeepAliveTimeOut => {
      logger.info("No keep alive request!!!!")
    }

    case ClientCloseConnection => {
      //TODO : will process handling
      logger.info("ClientCloseConnection : " + self.toString())
      cancelTimer
      connectionStatus = None
    }

  }

  def cancelTimer = {
    keepAliveTimer match {
      case Some(x) => x.cancel()
      case None =>
    }
  }

  def resetTimer = {
    cancelTimer
    connectionStatus match {
      case Some(x) =>
        keepAliveTimer = Some(ActorContainer.system.scheduler.scheduleOnce(
          FiniteDuration(x.keepAliveTime, TimeUnit.SECONDS), self, SessionKeepAliveTimeOut)
        )
      case None =>
    }
  }

  def mqttPacket(packet: Packet, bridge: ActorRef): Unit = {

    packet match {
      case connect: CONNECT =>
        logger.info("receive connect")
        connectionStatus = Some(ConnectionStatus(connect.will, connect.keepAlive.value))
        bridge ! MqttOutboundPacket(CONNACK(true, ReturnCode.connectionAccepted))
        resetTimer
      case PINGREQ =>
        resetTimer
        logger.info("receive pingreq")
        bridge ! MqttOutboundPacket(PINGRESP)
      case publish: PUBLISH => {
        logger.info(" publish : {}", publish.topic.value)
        ActorContainer.directory.tell(DirectoryReq(publish.topic.value, TOPIC),
          context.actorOf(Props(new Actor {
            def receive = {
              case DirectoryResp(topicName, option) =>
                option match {
                  case Some(x) => x ! TopicMessage(publish.data.value, publish.qos.value, publish.retain)
                  case None =>
                }
            }
          }))
        )
      }
      case DISCONNECT => {
        connectionStatus = None
        cancelTimer
      }

      case subscribe: SUBSCRIBE =>
        subscribeToTopics(subscribe.topicFilter)
        sender ! MqttOutboundPacket(SUBACK(subscribe.packetId ,subscribe.topicFilter.foldRight(List[BYTE]())((x,accum) => BYTE(0x00) ::accum )))


      case unsubscribe: UNSUBSCRIBE =>
        unsubscribeToTopics(unsubscribe.topicFilter)
    }

  }

  def subscribeToTopics(topicFilters: List[TopicFilter]) = {
    val clientId = self.path.name
    topicFilters.foreach(tp =>
      (ActorContainer.directory ? DirectoryReq(tp.topic.value, TOPIC)) onComplete {
        case Success(DirectoryResp(topicName, option: Option[ActorRef])) => option match {
          case Some(topicActor) => {
            logger.info("I will subscribe to actor({}) topicName({}) clientId({})",
              topicActor, tp.topic.value, tp
            )
            topicActor ! Subscribe(self.path.name)
          }
          case None => {
            logger.info("No topic actor topicName({}) clientId({})", tp.topic.value, self.path.name)
            ActorContainer.topicCreator.tell(topicName, context.actorOf(Props(new Actor {
              override def receive = {
                case topic: ActorRef => topic ! Subscribe(clientId)
              }
            })))
          }
        }
        case Failure(t) => {
          logger.info("Ask failure topicName({}) clientId({})", tp.topic.value, self.path.name)
        }
      }

    )

  }

  def unsubscribeToTopics(topics: List[STRING]) = {
    topics.foreach(tp =>
      (ActorContainer.directory ? DirectoryReq(tp.value, TOPIC)) onComplete {
        case Success(DirectoryResp(topicName, option: Option[ActorRef])) => option match {
          case Some(topicActor) => {
            logger.info("I will unsubscribe to actor({}) topicName({}) clientId({})",
              topicActor, tp.value, self.path.name
            )
            topicActor ! Unsubscribe(self.path.name)
          }
          case None => {
            logger.info("No topic actor topicName({}) clientId({})", tp.value, self.path.name)
          }
        }
        case Failure(t) => {
          logger.info("Ask failure topicName({}) clientId({})", tp.value, self.path.name)
          None
        }
      }
    )
  }

  def doConnect(connect: CONNECT): CONNACK = {
    CONNACK(true, ReturnCode.connectionAccepted)
  }

}
