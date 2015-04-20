package plantae.citrus.mqtt.dto.connect

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import plantae.citrus.mqtt.dto.{INT, STRING}

@RunWith(classOf[JUnitRunner])
class ConnectionTest extends FunSuite {

  trait ConnectPacketTest {
    val test = true
    val clientId = "client_id"

  }

  test("create connect packet") {
    new ConnectPacketTest {
      assert(test)


      val connectPacket =
        CONNECT(
          STRING(clientId), true,
          Option(null),
          Option(null),
          INT(60)
        )

      assert(connectPacket.clientId === STRING(clientId))
      assert(connectPacket.cleanSession === true)

    }
  }

  test("encode connect packet") {
    val encodedConnectPacket = CONNECT(
      STRING("client_id"), true,
      Option(Will(WillQos.QOS_1, true, STRING("testWillTopic"), STRING("TestWillMessage"))),
      Option(Authentication(STRING("id"), Option(STRING("password")))),
      INT(30000)
    ).encode

    assert(encodedConnectPacket === Array(16, 67, 0, 4, 77, 81, 84, 84, 4, -26, 117, 48, 0, 9, 99, 108, 105, 101, 110,
      116, 95, 105, 100, 0, 13, 116, 101, 115, 116, 87, 105, 108, 108, 84, 111, 112, 105, 99, 0, 15, 84, 101, 115, 116,
      87, 105, 108, 108, 77, 101, 115, 115, 97, 103, 101, 0, 2, 105, 100, 0, 8, 112, 97, 115, 115, 119, 111, 114, 100))

    val encodedConnectPacket2 = CONNECT(
      STRING("client_id"), true,
      Option(null),
      Option(null),
      INT(60)
    ).encode

    assert(encodedConnectPacket2 === Array(16, 21, 0, 4, 77, 81, 84, 84, 4, 2, 0, 60, 0, 9, 99, 108, 105, 101, 110, 116,
      95, 105, 100))
  }

  test("decode connect packet #1") {
    val encodedConnectPacket = Array[Byte](16, 21, 0, 4, 77, 81, 84, 84, 4, 2, 0, 60, 0, 9, 99, 108, 105, 101, 110, 116,
      95, 105, 100)
    val packet = CONNECTDecoder.decode(encodedConnectPacket)
    assert(packet.clientId === STRING("client_id"))
    assert(packet.cleanSession === true)

  }

  test("decode connect packet #2") {
    val min = CONNECTDecoder.decode(
      CONNECT(
        STRING("client_id"), true,
        Option(null),
        Option(null),
        INT(60)
      ).encode
    )

    assert(min.clientId === STRING("client_id"))
    assert(min.keepAlive === INT(60))

    val min_id = CONNECTDecoder.decode(
      CONNECT(
        STRING("client_id"), true,
        Option(null),
        Option(Authentication(STRING("id"), None)),
        INT(60)
      ).encode
    )
    assert(min_id.clientId === STRING("client_id"))
    assert(min_id.authentication.get.id === STRING("id"))
    assert(min_id.authentication.get.password === None)

    val min_pass = CONNECTDecoder.decode(
      CONNECT(
        STRING("client_id"), true,
        Option(null),
        Option(Authentication(STRING("id"), Option(STRING("password")))),
        INT(60)
      ).encode
    )

    assert(min_pass.authentication.get.id === STRING("id"))
    assert(min_pass.authentication.get.password === Some(STRING("password")))

    val will_message = STRING("will message will messagewill messagewill messagewill messagewill messagewill " +
      "messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill " +
      "message will messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill " +
      "messagewill messagewill messagewill messagewill messagewill message")

    val min_will = CONNECTDecoder.decode(
      CONNECT(
        STRING("client_id"), false,
        Option(Will(WillQos.QOS_3, true, STRING("will topic topic"), will_message)),
        Option(null),
        INT(60)
      ).encode
    )

    assert(min_will.will.get.qos === WillQos.QOS_3)
    assert(min_will.will.get.topic === STRING("will topic topic"))
    assert(min_will.will.get.retain === true)
    assert(min_will.will.get.message === will_message)
  }


}
