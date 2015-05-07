package plantae.citrus.mqtt.actors.session

import java.util.concurrent.TimeUnit

import akka.actor._
import plantae.citrus.mqtt.actors.SystemRoot
import plantae.citrus.mqtt.actors.directory._
import plantae.citrus.mqtt.actors.topic.TopicInMessage
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

  def cancelTimer = {
    keepAliveTimer.cancel()
  }

  def resetTimer = {
    cancelTimer
    keepAliveTimer = SystemRoot.system.scheduler.scheduleOnce(
      FiniteDuration(keepAliveTime, TimeUnit.SECONDS), session, SessionKeepAliveTimeOut)
  }

  private def publishWill = {
    will match {
      case Some(x) =>
        SystemRoot.directoryProxy.tell(DirectoryTopicRequest(x.topic), sessionContext.actorOf(Props(new Actor {
          def receive = {
            case DirectoryTopicResult(name, qos, actors) =>
              val topicInMessage = TopicInMessage(x.message.toArray, x.qos, x.retain, x.qos match {
                case 0 => None
                case qos if (qos > 0) => Some(0)
              })

              actors.foreach {
                _.tell(topicInMessage, ActorRef.noSender)
              }

              context.stop(self)
          }
        }
        )))

      case None =>
    }
  }

  def destroyProperly = {
    sessionContext.stop(socket)
    cancelTimer
  }

  def destroyAbnormally = {
    sessionContext.stop(socket)
    publishWill
    cancelTimer
  }


}
