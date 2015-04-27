package plantae.citrus.exercise

import scala.collection.mutable.Map

object TestApp extends App {
  val root = DiskTreeNode[TempTopic]("", "", Map[String, DiskTreeNode[TempTopic]]())

  root.addNode("a/b/c1", TempTopic("a/b/c1"))
  root.addNode("a/b/c2", TempTopic("a/b/c2"))
  root.addNode("a/b/c3/d", TempTopic("a/b/c3/d"))
  root.addNode("a/b/c4/d", TempTopic("a/b/c4/d"))
  root.addNode("a/b/c5/d", TempTopic("a/b/c5/d"))
  root.addNode("1/2", TempTopic("1/2"))
  root.addNode("1/3", TempTopic("1/3"))
  root.addNode("x/y", TempTopic("x/y"))

  printList(root.getNodes("a/b/c1"))

  printList(root.getNodes("1/2"))
  printList(root.getNodes("1"))
  printList(root.getNodes("1/2/3"))
  printList(root.getNodes("a/b/+/d"))
  printList(root.getNodes("a/b/+"))
  printList(root.getNodes("a/*"))

  def printList(list: List[TempTopic]): Unit ={
    list.foreach(tt => println(tt))
    println("====")
  }

}

case class TempTopic(name: String) {
  def printName = {
    println(name)
  }
}

case class DiskTreeNode[A](name: String, fullPath: String, children: Map[String, DiskTreeNode[A]] = Map[String, DiskTreeNode[A]]()){
  var topic: Option[A] = None

  def pathToList(path: String) : List[String] = {
    path.split("/").toList
  }

  def addNode(path: String, topic: A): Boolean = {
    addNode(pathToList(path), path, topic)
  }

  def addNode(paths: List[String], path: String, topic: A): Boolean = {
    paths match {
      case Nil => this.topic = Some(topic)
      case _ => {
        children.get(paths.head) match {
          case Some(node: DiskTreeNode[A]) => {
            node.addNode(paths.tail, path, topic)
          }
          case None => {
            val node = new DiskTreeNode[A](paths.head, fullPath +"/" +paths.head)
            node.addNode(paths.tail, path, topic)
            children.+=((paths.head, node))
          }
        }
      }
    }

    true
  }

  def removeNode(path: String): Boolean = {
    removeNode(pathToList(path))
  }

  def removeNode(paths: List[String]): Boolean = {
    if(paths.size == 1){
      children.-(paths.head)
    } else if(paths.size > 1) {
      children.get(paths.head) match {
        case Some(node: DiskTreeNode[A]) => {
          node.removeNode(paths.tail)
        }
      }
    }

    true
  }

  def getNodes(path: String): List[A] = {
    getNodes(pathToList(path))
  }

  def getNodes(paths: List[String]) : List[A] = {
    paths match {
      case x :: Nil => {
        x match {
          case "*" => getEveryNodes()
          case "+" => {
            children.filter(x => x._2.topic.isDefined).map(y => y._2.topic match {
              case Some(y:A) => y
            }).toList
          }
          case _ => {
            children.get(x) match {
              case Some(node:DiskTreeNode[A]) => {
                node.topic match {
                  case Some(t) => List(t)
                  case None => List()
                }

              }
              case None => List()
            }
          }
        }
      }
      case x :: others => {
        x match {
          case "+" => {
            children.map(x => {
              x._2.getNodes(others)
            }).flatten.toList
          }
          case _ => {
            children.get(x) match {
              case Some(node: DiskTreeNode[A]) => node.getNodes(others)
              case None => List()
            }
          }
        }
      }
    }
  }

  def getEveryNodes(): List[A] = {
    val topics = children.map(x => {
      x._2.getEveryNodes()
    }).flatten.toList

    topic match {
      case Some(x: A) => x :: topics
      case None => topics
    }
  }
}
