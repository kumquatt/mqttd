package plantae.citrus.mqtt.actors.topic

//import plantae.citrus.mqtt.actors.connection.Session
//
//import scala.collection.mutable
//import scala.collection.mutable.Map
//
//
//object test extends App {
//  println("Hello world")
////  val root = DiskTreeNode2[TempClass]("", Map[String, DiskTreeNode2[TempClass]]())
//  val root = MyTreeNode[String](elem = MyElement[String](name = ""))
//  root.addNode("a/b/+".split("/").toList, "cli1")
//  root.addNode("a/b/+".split("/").toList, "cli2")
//  root.addNode("a/b/+".split("/").toList, "cli3")
//  root.addNode("a/+/1".split("/").toList, "cli4")
//  root.addNode("a/+/2".split("/").toList, "cli4")
//  root.addNode("a/+/3".split("/").toList, "cli4")
//  root.addNode("+/+".split("/").toList, "cli5")
//  root.addNode("b/#".split("/").toList, "cli6")
//  root.addNode("b/#".split("/").toList, "cli6")
//
//  println("hello world")
//
//  println(root.getSubscribers("a/b/c".split("/").toList))
//  println(root.getSubscribers("a/b/1".split("/").toList))
//  println(root.getSubscribers("b/c/d/e/f/g".split("/").toList))
//  println(root.getSubscribers("a/b".split("/").toList))
//  println(root.getSubscribers("x/y/z".split("/").toList))
//
//}
//
//case class MyElement[T](name: String, subscribers: mutable.Set[T] = mutable.Set[T]()){
//  def subscribe(arg: T): Unit = {
//    subscribers.add(arg)
//  }
//
//}
//
//case class MyTreeNode[T](
//                    val elem: MyElement[T],
//                    val children: collection.mutable.Map[String, MyTreeNode[T]] = collection.mutable.Map[String, MyTreeNode[T]]()
//                      ) {
//  def addNode(pathes: List[String], subscribe: T): Unit ={
//    pathes match {
//      case Nil =>
//      case x :: Nil =>{
//        val node = children.get(x) match {
//          case Some(node) => node
//          case None => {
//            val node = MyTreeNode[T](MyElement(x))
//            children.put(x, node)
//            node
//          }
//        }
//        node.elem.subscribe(subscribe)
//
//      }
//      case x :: others =>{
//        val node = children.get(x) match {
//          case Some(node) => node
//          case None => {
//            val node = MyTreeNode[T](MyElement(x))
//            children.put(x, node)
//            node
//          }
//        }
//        node.addNode(others, subscribe)
//      }
//    }
//
//  }
//
//  def getNode(pathes: List[String]): Option[MyTreeNode[T]] = {
//    pathes match {
//      case Nil => None
//      case x :: Nil => {
//        children.get(x) match {
//          case Some(node) => Some(node)
//          case None => None
//        }
//      }
//      case x :: others => {
//        children.get(x) match {
//          case Some(node) => node.getNode(others)
//          case None => None
//        }
//      }
//    }
//  }
//
//  def getSubscribers(pathes: List[String]) : List[T] = {
//    pathes match {
//      case Nil => List.empty
//      case x :: Nil => {
//        val l1 = children.get("+") match {
//          case Some(node) => node :: Nil
//          case None  => Nil
//        }
//
//        val l2 = children.get(x) match {
//          case Some(node) => node :: Nil
//          case None => Nil
//        }
//
//        val l3 = children.get("#") match {
//          case Some(node) => node :: Nil
//          case None => Nil
//        }
//
//        val list: List[MyTreeNode[T]] = l1 ::: l2 ::: l3
//        list.map(l => l.elem.subscribers.toList).flatten
//      }
//      case x :: others => {
//        val l1 = children.get(x) match {
//          case Some(node) => node.getSubscribers(others)
//          case None => Nil
//        }
//
//        val l2 = children.get("+") match {
//          case Some(node) => node.getSubscribers(others)
//          case None => Nil
//        }
//
//        val wild = children.get("#") match {
//          case Some(node) => node.elem.subscribers.toList
//          case None => Nil
//        }
//
//        l1 ::: l2 ::: wild
//      }
//    }
//  }
//
//
//}
