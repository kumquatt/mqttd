package plantae.citrus.mqtt.actors.topic

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import scala.collection.mutable.Map

@RunWith(classOf[JUnitRunner])
class TopicTest extends FunSuite {
  test("DiskTreeNode test") {
    case class TestTopic(name: String)

    val root = DiskTreeNode[TestTopic]("", "",
      Map[String, DiskTreeNode[TestTopic]]()
    )

    root.addNode("a/b/c1", TestTopic("a/b/c1"))
    root.addNode("a/b/c2", TestTopic("a/b/c2"))
    root.addNode("a/b/c3/d", TestTopic("a/b/c3/d"))
    root.addNode("a/b/c4/d", TestTopic("a/b/c4/d"))
    root.addNode("a/b/c5/d", TestTopic("a/b/c5/d"))
    root.addNode("1/2", TestTopic("1/2"))
    root.addNode("1/3", TestTopic("1/3"))
    root.addNode("x/y", TestTopic("x/y"))


    assert(root.getNodes("a/b/c1") === List(TestTopic("a/b/c1")))
    assert(root.getNodes("1/2") === List(TestTopic("1/2")))
    assert(root.getNodes("1") === List())
    assert(root.getNodes("1/2/3") === List())
    assert(root.getNodes("a/b/+/d").size === 3)
    assert(root.getNodes("a/b/+").size === 2)
    assert(root.getNodes("a/*").size === 5)
  }

}
