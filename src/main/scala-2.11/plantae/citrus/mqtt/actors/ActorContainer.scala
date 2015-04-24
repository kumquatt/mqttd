package plantae.citrus.mqtt.actors

import akka.actor._
import com.typesafe.config.ConfigFactory
import plantae.citrus.mqtt.actors.directory._
import plantae.citrus.mqtt.actors.session.SessionCreator
import plantae.citrus.mqtt.actors.topic.TopicCreator

/**
 * Created by yinjae on 15. 4. 21..
 */
object ActorContainer {
  val system = ActorSystem("mqtt", ConfigFactory.load.getConfig("mqtt"))
  val directory = system.actorOf(Props[Directory], "directory")
  val sessionCreator = system.actorOf(Props[SessionCreator], "session")
  val topicCreator = system.actorOf(Props[TopicCreator], "topic")
  val directoryProxyMaster = system.actorOf(Props[DirectoryProxyMaster], "directoryProxy")

  def directoryOperation(x: DirectoryOperation, senderContext: ActorContext, originalSender: ActorRef) = {

    directoryProxyMaster.tell(GetDirectoryActor, senderContext.actorOf(Props(new Actor {
      override def receive = {
        case DirectoryActor(actor) => actor.tell(x, originalSender)
          context.stop(self)

      }
    })))
  }

  def invokeCallback(directoryReq: DirectoryReq, senderContext: ActorContext, callback: PartialFunction[Any, Unit]): Unit = {

    directoryProxyMaster.tell(GetDirectoryActor, senderContext.actorOf(Props(new Actor {
      override def receive = {
        case DirectoryActor(actor) =>
          val callbackInvoker = context.actorOf(Props(new Actor {
            override def postStop = {
            }

            override def receive = callback
          }))
          context.watch(callbackInvoker)
          actor.tell(directoryReq, callbackInvoker)
        case Terminated(x) =>
          println(x + " really is dead")
      }
    })))
  }
}

