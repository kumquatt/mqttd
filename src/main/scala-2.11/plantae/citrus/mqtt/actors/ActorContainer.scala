package plantae.citrus.mqtt.actors

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

/**
 * Created by yinjae on 15. 4. 21..
 */
object ActorContainer {
  val system = ActorSystem("mqtt", ConfigFactory.load.getConfig("mqtt"))
<<<<<<< HEAD
  val directory = system.actorOf(Props[Directory], "directory")
  val sessionCreator = system.actorOf(Props[SessionCreator], "session")
=======
  val directory = system.actorOf(Props[Directory])
  val topicDirectory = system.actorOf(Props[TopicDirectory])
>>>>>>> 81c10681884e05d8005978aad404136e67516114
}

