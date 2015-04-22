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

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext}


case class MqttInboundPacket(mqttPacket: Packet)

case class MqttOutboundPacket(mqttPacket: Packet)

class SessionCommand

case object SessionReset extends SessionCommand

case object SessionResetAck extends SessionCommand

case object SessionKeepAliveTimeOut extends SessionCommand

case object ClientCloseConnection extends SessionCommand

class SessionCreator extends Actor {
  private val logger = Logging(context.system, this)

  override def receive = {
    case clientId: String => {
      logger.info("new session is created [{}]", clientId)
      sender ! context.actorOf(Props[Session], clientId)
    }
  }
}

class Session extends DirectoryMonitorActor {
  implicit val timeout = akka.util.Timeout(5, TimeUnit.SECONDS)
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private val logger = Logging(context.system, this)

  case class ConnectionStatus(will: Option[Will], keepAliveTime: Int)

  var connectionStatus: Option[ConnectionStatus] = None
  var keepAliveTimer: Option[Cancellable] = None


  override def actorType: ActorType = TypeSession

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
        ActorContainer.directory.tell(DirectoryReq(publish.topic.value, TypeTopic),
          context.actorOf(Props(new Actor {
            def receive = {
              case DirectoryResp(topicName, option) =>
                option ! TopicMessage(publish.data.value, publish.qos.value, publish.retain)
            }
          }))
        )
      }
      case DISCONNECT => {
        connectionStatus = None
        cancelTimer
      }

      case subscribe: SUBSCRIBE =>
        val subscribeResult = subscribeToTopics(subscribe.topicFilter)

        subscribeResult.foreach(b =>
          logger.info("... " + b)
        )
        sender ! MqttOutboundPacket(SUBACK(subscribe.packetId, subscribeResult))
      //        sender ! MqttOutboundPacket(SUBACK(subscribe.packetId ,subscribe.topicFilter.foldRight(List[BYTE]())((x,accum) => BYTE(0x00) ::accum )))


      case unsubscribe: UNSUBSCRIBE =>
        unsubscribeToTopics(unsubscribe.topicFilter)
    }

  }

  def subscribeToTopics(topicFilters: List[TopicFilter]): List[BYTE] = {
    val clientId = self.path.name

    val result = topicFilters.map(tp =>
      Await.result(ActorContainer.directory ? DirectoryReq(tp.topic.value, TypeTopic), Duration.Inf) match {
        case DirectoryResp(topicName, option) =>
          option ! Subscribe(self.path.name)
          BYTE(0x00)
      }
    )
    result
  }

  def unsubscribeToTopics(topics: List[STRING]) = {
  }

  def doConnect(connect: CONNECT): CONNACK = {
    CONNACK(true, ReturnCode.connectionAccepted)
  }

}
