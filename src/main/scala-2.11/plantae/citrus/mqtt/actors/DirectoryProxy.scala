package plantae.citrus.mqtt.actors

import akka.actor._
import akka.event.Logging
import akka.routing.{Router, RoundRobinRoutingLogic, ActorRefRoutee}

case object GetDirectoryActor
case class DirectoryActor(actor: ActorRef)

class DirectoryProxyMaster extends Actor {
  private val logger = Logging(context.system, this)

  var router = {
    val routees = Vector.fill(5) {
      val r = context.actorOf(Props[DirectoryProxyWorker])
      context watch r
      ActorRefRoutee(r)
    }

    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case GetDirectoryActor => router.route(GetDirectoryActor, sender)

    case Terminated(a) =>
      router = router.removeRoutee(a)
      val r = context.actorOf(Props[DirectoryProxyWorker])
      context watch r
      router = router.addRoutee(r)
  }
}

class DirectoryProxyWorker extends Actor {
  private val logger = Logging(context.system, this)

  def receive = {
    case GetDirectoryActor =>
      logger.info("Message received")
      val actor = ActorContainer.directory
      sender() ! DirectoryActor(actor)
  }
}

//class TestActor extends Actor {
//  private val logger = Logging(context.system, this)
//  val system = ActorSystem("testRouter")
//  val directoryProxyMaster = system.actorOf(Props[DirectoryProxyMaster], "directoryProxyMaster")
//
//  def receive = {
//    case clientId: String =>
//      logger.info("start to send message")
//      directoryProxyMaster ! SomeReq(clientId)
//    case ack : SomeAck =>
//      logger.info("received message {}", ack.msgId)
//  }
//}
//object DirectoryProxyTest extends App {
//  val system = ActorSystem("testRouter")
//  val testActor = system.actorOf(Props[TestActor], "testActor")
//  testActor ! "11111"
//  testActor ! "22222"
//  testActor ! "33333"
//  testActor ! "44444"
//  testActor ! "55555"
//  testActor ! "66666"
//  testActor ! "77777"
//  testActor ! "88888"
//
//
//}