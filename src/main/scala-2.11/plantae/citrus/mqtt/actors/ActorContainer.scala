package plantae.citrus.mqtt.actors

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import plantae.citrus.mqtt.actors.directory.{DirectoryProxyMaster, Directory}
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
}

