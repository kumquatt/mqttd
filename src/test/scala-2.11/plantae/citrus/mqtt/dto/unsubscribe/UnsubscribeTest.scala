
package plantae.citrus.mqtt.dto.unsubscribe

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import plantae.citrus.mqtt.dto.{INT, STRING}

@RunWith(classOf[JUnitRunner])
class UnsubscribeTest extends FunSuite {


  test("create unsubscribe packet") {
    val unsubscribe =
      UNSUBSCRIBE(

        INT(40293.toShort),
        List(STRING("topic/1"), STRING("topic/1"), STRING("topic/1"), STRING("topic/1"))
      )

    assert(unsubscribe.packetId == INT(40293.toShort))
    assert(unsubscribe.topicFilter === List(STRING("topic/1"), STRING("topic/1"), STRING("topic/1"), STRING("topic/1")))
  }

  test("encode/decode subscribe packet") {
    val unsubscribe = UNSUBSCRIBEDecoder.decode(
      UNSUBSCRIBE(
        INT(40293.toShort),
        List(STRING("topic/1"), STRING("topic/1"), STRING("topic/1"), STRING("topic/1"))
      ).encode
    )

    assert(unsubscribe.packetId == INT(40293.toShort))
    assert(unsubscribe.topicFilter === List(STRING("topic/1"), STRING("topic/1"), STRING("topic/1"), STRING("topic/1")))
  }
  test("create unsuback packet") {
    val unsuback =
      UNSUBACK(

        INT(40293.toShort)
      )
    assert(unsuback.packetId == INT(40293.toShort))
  }

  test("encode/decode unsuback packet") {
    val unsuback = UNSUBACKDecoder.decode(
      UNSUBACK(

        INT(40293.toShort)
      ).encode
    )
    assert(unsuback.packetId == INT(40293.toShort))
  }


}
