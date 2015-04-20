
package plantae.citrus.mqtt.dto.subscribe

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import plantae.citrus.mqtt.dto.{BYTE, INT, STRING}

@RunWith(classOf[JUnitRunner])
class SubscribeTest extends FunSuite {


  test("create subscribe packet") {
    val subscribe =
      SUBSCRIBE(

        INT(40293.toShort),
        List(TopicFilter(STRING("topic/1"), BYTE(0x00)),
          TopicFilter(STRING("topic/2"), BYTE(0x01)),
          TopicFilter(STRING("topic/3"), BYTE(0x02)),
          TopicFilter(STRING("topic/4"), BYTE(0x00)))
      )

    assert(subscribe.packetId == INT(40293.toShort))
    assert(subscribe.topicFilter === List(TopicFilter(STRING("topic/1"), BYTE(0x00)),
      TopicFilter(STRING("topic/2"), BYTE(0x01)),
      TopicFilter(STRING("topic/3"), BYTE(0x02)),
      TopicFilter(STRING("topic/4"), BYTE(0x00))))
  }

  test("encode/decode subscribe packet") {
    val subscribe = SUBSCRIBEDecoder.decode(
      SUBSCRIBE(

        INT(40293.toShort),
        List(TopicFilter(STRING("topic/1"), BYTE(0x00)),
          TopicFilter(STRING("topic/2"), BYTE(0x01)),
          TopicFilter(STRING("topic/3"), BYTE(0x02)),
          TopicFilter(STRING("topic/4"), BYTE(0x00)))
      ).encode
    )

    assert(subscribe.packetId == INT(40293.toShort))
    assert(subscribe.topicFilter === List(TopicFilter(STRING("topic/1"), BYTE(0x00)),
      TopicFilter(STRING("topic/2"), BYTE(0x01)),
      TopicFilter(STRING("topic/3"), BYTE(0x02)),
      TopicFilter(STRING("topic/4"), BYTE(0x00))))
  }

  test("create suback packet") {
    val suback =
      SUBACK(

        INT(40293.toShort),
        List(BYTE(0x00), BYTE(0x01), BYTE(0x02), BYTE(0x03))
      )
    assert(suback.packetId == INT(40293.toShort))
    assert(suback.returnCode === List(BYTE(0x00), BYTE(0x01), BYTE(0x02), BYTE(0x03)))
  }

  test("encode/decode suback packet") {
    val suback =
      SUBACKDecoder.decode(SUBACK(

        INT(40293.toShort),
        List(BYTE(0x00), BYTE(0x01), BYTE(0x02), BYTE(0x03))
      ).encode
      )
    assert(suback.packetId == INT(40293.toShort))
    assert(suback.returnCode === List(BYTE(0x00), BYTE(0x01), BYTE(0x02), BYTE(0x03)))
  }


}
