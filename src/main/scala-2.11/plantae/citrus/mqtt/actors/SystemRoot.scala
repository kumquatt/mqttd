package plantae.citrus.mqtt.actors

import java.net.InetAddress

import akka.actor._
import com.typesafe.config.ConfigFactory
import plantae.citrus.mqtt.actors.directory._
import plantae.citrus.mqtt.actors.session.SessionRoot
import plantae.citrus.mqtt.actors.topic.TopicRoot

/**
 * Created by yinjae on 15. 4. 21..
 */
object SystemRoot {


  val config = {
    val config = ConfigFactory.load()
    val builder = new StringBuilder
    if (config.getString("akka.remote.netty.tcp.hostname") == null) {
      builder.append("akka.remote.netty.tcp.hostname = " + {
        InetAddress.getAllByName(InetAddress.getLocalHost().getCanonicalHostName()) match {
          case null => "127.0.0.1"
          case Array() => "127.0.0.1"
          case hostNames: Array[InetAddress] => hostNames.filterNot(x => x.getHostAddress.startsWith("127")) match {
            case Array() => "127.0.0.1"
            case other: Array[InetAddress] => other(0).getHostAddress
          }
        }
      })
    }
    println("akka.remote.netty.tcp.hostname is " + config.getString("akka.remote.netty.tcp.hostname"))
    config.withFallback(ConfigFactory.parseString(builder.toString()))
  }
  val system = ActorSystem("mqtt", config.getConfig("mqtt"))
  val sessionRoot = system.actorOf(Props[SessionRoot], "session")
  val topicRoot = system.actorOf(Props[TopicRoot], "topic")
  val directoryProxy = system.actorOf(Props[DirectoryProxy], "directory")


  def directoryOperation(x: DirectoryOperation, senderContext: ActorContext, originalSender: ActorRef) = {
    implicit val context: ActorContext = senderContext
    directoryProxy.forward(x)
  }


  def invokeCallback(directoryReq: DirectoryReq, senderContext: ActorContext, props: Props) = {
    directoryProxy.tell(directoryReq, senderContext.actorOf(props))
  }
}

