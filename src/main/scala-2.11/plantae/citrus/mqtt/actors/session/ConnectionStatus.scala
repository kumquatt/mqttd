package plantae.citrus.mqtt.actors.session

import java.util.concurrent.TimeUnit

import akka.actor._
import plantae.citrus.mqtt.actors.SystemRoot
import plantae.citrus.mqtt.actors.topic.Publish
import plantae.citrus.mqtt.dto.connect.Will

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
 * Created by yinjae on 15. 4. 24..
 */
case class ConnectionStatus(will: Option[Will], keepAliveTime: Int, session: ActorRef, sessionContext: ActorContext, socket: ActorRef) {
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private var keepAliveTimer: Option[Cancellable] = if (keepAliveTime > 0)
    Some(SystemRoot.system.scheduler.scheduleOnce(FiniteDuration(keepAliveTime, TimeUnit.SECONDS), session, SessionKeepAliveTimeOut))
  else None

  def cancelTimer = {
    keepAliveTimer match {
      case Some(x) => x.cancel()
      case None =>
    }

  }

  def resetTimer = {
    cancelTimer
    keepAliveTimer = if (keepAliveTime > 0)
      Some(SystemRoot.system.scheduler.scheduleOnce(FiniteDuration(keepAliveTime, TimeUnit.SECONDS), session, SessionKeepAliveTimeOut))
    else None
  }

  private def publishWill = {
    will match {
      case Some(x) =>
      // TODO : will qos
        SystemRoot.topicManager! Publish(x.topic, x.message, x.retain, None)
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
