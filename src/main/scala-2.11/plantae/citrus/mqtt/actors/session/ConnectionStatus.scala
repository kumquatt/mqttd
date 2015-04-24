package plantae.citrus.mqtt.actors.session

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, Cancellable}
import plantae.citrus.mqtt.actors.ActorContainer
import plantae.citrus.mqtt.actors.directory.{DirectoryReq, DirectoryResp, TypeTopic}
import plantae.citrus.mqtt.actors.topic.TopicInMessage
import plantae.citrus.mqtt.dto.BYTE
import plantae.citrus.mqtt.dto.connect.Will

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
 * Created by yinjae on 15. 4. 24..
 */
case class ConnectionStatus(will: Option[Will], keepAliveTime: Int, session: ActorRef, socket: ActorRef) {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private var keepAliveTimer: Cancellable = ActorContainer.system.scheduler.scheduleOnce(
    FiniteDuration(keepAliveTime, TimeUnit.SECONDS), session, SessionKeepAliveTimeOut)

  private var publishActor: Option[ActorRef] = None

  def cancelTimer = {
    keepAliveTimer.cancel()
  }

  def resetTimer = {
    cancelTimer
    keepAliveTimer = ActorContainer.system.scheduler.scheduleOnce(
      FiniteDuration(keepAliveTime, TimeUnit.SECONDS), session, SessionKeepAliveTimeOut)
  }

  def handleWill = {
    will match {
      case Some(x) =>
        ActorContainer.invokeCallback(DirectoryReq(x.topic.value, TypeTopic), {
          case DirectoryResp(name, actor) =>
            val topicInMessage = TopicInMessage(x.message.value.getBytes, (x.qos >> 3 & BYTE(0x03.toByte)).value.toShort, x.retain, x.qos.value.toShort match {
              case 0 => None
              case qos if (qos > 0) => Some(1)
            })
            actor.tell(topicInMessage, ActorRef.noSender)
        }
        )
      case None =>
    }
  }

  def destory = {
    cancelTimer
  }
}
