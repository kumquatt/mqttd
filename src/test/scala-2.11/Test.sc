import java.io.File

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.{ConfigParseOptions, ConfigFactory}
import plantae.citrus.mqtt.actors.directory.{DirectorySessionResult, DirectoryReq, TypeSession}

new File("hello").getAbsolutePath
val config = ConfigFactory.load(ConfigFactory.parseFile(new File("/Users/yinjae/IdealProjects/dev/kumquatt/src/test/resources/application.conf")))

config.getConfig("mqtt")

val system = ActorSystem("mqtt", config)

system.actorOf(Props(new Actor with ActorLogging {
  override def receive: Receive = {
    case str: String =>
      context.actorSelection("akka.tcp://mqtt@127.0.0.1:30000/user/directory") ! DirectoryReq(str, TypeSession)
    case DirectorySessionResult(name, ref) =>
      log.info("receive name:{} ref : {}" ,name, ref )
  }
})) ! "testSession1"

