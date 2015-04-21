package plantae.citrus.mqtt.actors

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

/**
 * Created by yinjae on 15. 4. 21..
 */
object ActorContainer {
  val system = ActorSystem("mqtt", ConfigFactory.load.getConfig("mqtt"))
  val directory = system.actorOf(Props[Directory])
}
