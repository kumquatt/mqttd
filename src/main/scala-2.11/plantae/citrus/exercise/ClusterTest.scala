package plantae.citrus.exercise

import akka.actor.{ActorLogging, Actor, Props}
import plantae.citrus.mqtt.actors.SystemRoot
import plantae.citrus.mqtt.actors.directory.{DirectorySessionResult, TypeSession, DirectoryReq}

/**
 * Created by yinjae on 15. 4. 30..
 */
object ClusterTest extends App{
  SystemRoot.system.actorOf(Props(new Actor with ActorLogging {
    override def receive: Receive = {
      case str: String =>
        context.actorSelection("akka.tcp://mqtt@127.0.0.1:30000/user/directory") ! DirectoryReq(str, TypeSession)
      case DirectorySessionResult(name, ref) =>
        log.info("receive name:{} ref : {}" ,name, ref )
    }
  })) ! "testSession1"


}
