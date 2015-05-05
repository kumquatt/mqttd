package plantae.citrus.exercise

import akka.actor.{Actor, ActorLogging, Props}
import plantae.citrus.mqtt.actors.SystemRoot
import plantae.citrus.mqtt.actors.directory.{DirectorySessionResult, DirectoryTopicRequest}

/**
 * Created by yinjae on 15. 4. 30..
 */
object ClusterTest extends App {
  SystemRoot.system.actorOf(Props(new Actor with ActorLogging {
    override def receive: Receive = {
      case str: String =>
        context.actorSelection("akka.tcp://mqtt@127.0.0.1:30000/user/directory") ! DirectoryTopicRequest(str)
      case DirectorySessionResult(name, ref) =>
        log.info("receive name:{} ref : {}", name, ref)
    }
  })) ! "testSession1"


}
