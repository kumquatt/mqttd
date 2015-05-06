package plantae.citrus.mqtt.actors.session

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class StorageTest extends FunSuite {
  test("persist test") {
    val storage = Storage("persist-test")
    Range(1, 2000).foreach(count => {
      storage.persist((count + " persist").getBytes, (count % 3).toShort, true, "topic" + count)
    })

    assert(
      !Range(1, 2000).exists(count => {
        storage.nextMessage match {
          case Some(message) =>
            storage.complete(message.packetId match {
              case Some(x) => Some(x)
              case None => None
            })
            println(new String(message.payload.toArray))
            count + " persist" != new String(message.payload.toArray)

          case None => true
        }
      })
    )
  }
}
