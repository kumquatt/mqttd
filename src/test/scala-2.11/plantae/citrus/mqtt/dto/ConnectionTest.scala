package plantae.citrus.mqtt.dto

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
 * Created by before30 on 15. 4. 18..
 */

@RunWith(classOf[JUnitRunner])
class ConnectionTest extends FunSuite {

  trait ConnectPacketTest {
    val test = true

  }

  test("encode connect packet") {
    new ConnectPacketTest {
      assert(test === true)
    }
  }
}
