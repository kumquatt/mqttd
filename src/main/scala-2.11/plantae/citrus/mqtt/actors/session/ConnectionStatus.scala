package plantae.citrus.mqtt.actors.session

import java.util.concurrent.TimeUnit

import akka.actor._
import plantae.citrus.mqtt.actors.SystemRoot
import plantae.citrus.mqtt.actors.directory._
import plantae.citrus.mqtt.actors.topic.TopicInMessage
import plantae.citrus.mqtt.dto.BYTE
import plantae.citrus.mqtt.dto.connect.Will

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
 * Created by yinjae on 15. 4. 24..
 */
case class ConnectionStatus(will: Option[Will], keepAliveTime: Int, session: ActorRef, sessionContext: ActorContext, socket: ActorRef) {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private var keepAliveTimer: Cancellable = SystemRoot.system.scheduler.scheduleOnce(
    FiniteDuration(keepAliveTime, TimeUnit.SECONDS), session, SessionKeepAliveTimeOut)

  private var publishActor: Option[ActorRef] = None

  def cancelTimer = {
    keepAliveTimer.cancel()
  }

  def resetTimer = {
    cancelTimer
    keepAliveTimer = SystemRoot.system.scheduler.scheduleOnce(
      FiniteDuration(keepAliveTime, TimeUnit.SECONDS), session, SessionKeepAliveTimeOut)
  }

  def handleWill = {
    will match {
      case Some(x) =>
        SystemRoot.directoryProxy.tell(DirectoryTopicRequest(x.topic.value), sessionContext.actorOf(Props(new Actor {
          def receive = {
            case DirectoryTopicResult(name, actors) =>
              val topicInMessage = TopicInMessage(x.message.value.getBytes, (x.qos >> 3 & BYTE(0x03.toByte)).value.toShort, x.retain, x.qos.value.toShort match {
                case 0 => None
                case qos if (qos > 0) => Some(1)
              })
              actors.foreach {
                _.tell(topicInMessage, ActorRef.noSender)
              }
          }
        }
        )))

      case None =>
    }
  }

  def destory = {
    cancelTimer
  }
}
