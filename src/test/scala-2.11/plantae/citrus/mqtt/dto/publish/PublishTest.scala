
package plantae.citrus.mqtt.dto.publish

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import plantae.citrus.mqtt.dto.{INT, PUBLISHPAYLOAD, STRING}

@RunWith(classOf[JUnitRunner])
class PublishTest extends FunSuite {


  test("create publish packet") {
    val connectPacket =
      PUBLISH(
        false, INT(3),
        false,
        STRING("test/topic"),
        INT(40293.toShort),
        PUBLISHPAYLOAD(Array(0x1.toByte, 0x2.toByte, 0x3.toByte, 0x4.toByte, 0x5.toByte))
      )

    assert(connectPacket.dup == false)
    assert(connectPacket.qos == INT(3))
    assert(connectPacket.retain == false)
    assert(connectPacket.packetId === INT(40293.toShort))
    assert(connectPacket.topic === STRING("test/topic"))
    assert(connectPacket.payload.encode === PUBLISHPAYLOAD(Array(0x1.toByte, 0x2.toByte, 0x3.toByte, 0x4.toByte, 0x5.toByte)).encode)
  }


  test("create publish packet - retain & dup") {
    val connectPacket =
      PUBLISH(
        true, INT(2),
        true,
        STRING("test/topic"),
        INT(40293.toShort),
        PUBLISHPAYLOAD(Array(0x1.toByte, 0x2.toByte, 0x3.toByte, 0x4.toByte, 0x5.toByte))
      )

    assert(connectPacket.dup == true)
    assert(connectPacket.qos == INT(2))
    assert(connectPacket.retain == true)
    assert(connectPacket.packetId === INT(40293.toShort))
    assert(connectPacket.topic === STRING("test/topic"))
    assert(connectPacket.payload.encode === PUBLISHPAYLOAD(Array(0x1.toByte, 0x2.toByte, 0x3.toByte, 0x4.toByte, 0x5.toByte)).encode)
  }

  test("create publish packet - empty payload") {
    val connectPacket =
      PUBLISH(
        false, INT(3),
        false,
        STRING("test/topic"),
        INT(40293.toShort),
        PUBLISHPAYLOAD(Array())
      )

    assert(connectPacket.dup == false)
    assert(connectPacket.qos == INT(3))
    assert(connectPacket.retain == false)
    assert(connectPacket.packetId === INT(40293.toShort))
    assert(connectPacket.topic === STRING("test/topic"))
    assert(connectPacket.payload.encode === PUBLISHPAYLOAD(Array()).encode)
  }


  test("encode/decode publish packet") {
    val connectPacket = PUBLISHDecoder.decode(
      PUBLISH(
        false, INT(3),
        false,
        STRING("test/topic"),
        INT(40293.toShort),
        PUBLISHPAYLOAD(Array(0x1.toByte, 0x2.toByte, 0x3.toByte, 0x4.toByte, 0x5.toByte))
      ).encode
    )

    assert(connectPacket.dup == false)
    assert(connectPacket.qos == INT(3))
    assert(connectPacket.retain == false)
    assert(connectPacket.packetId === INT(40293.toShort))
    assert(connectPacket.topic === STRING("test/topic"))
    assert(connectPacket.payload.encode === PUBLISHPAYLOAD(Array(0x1.toByte, 0x2.toByte, 0x3.toByte, 0x4.toByte, 0x5.toByte)).encode)
  }


  test("encode/decode publish packet - retain & dup") {
    val connectPacket = PUBLISHDecoder.decode(PUBLISH(
      true, INT(2),
      true,
      STRING("test/topic"),
      INT(40293.toShort),
      PUBLISHPAYLOAD(Array(0x1.toByte, 0x2.toByte, 0x3.toByte, 0x4.toByte, 0x5.toByte))
    ).encode
    )

    assert(connectPacket.dup == true)
    assert(connectPacket.qos == INT(2))
    assert(connectPacket.retain == true)
    assert(connectPacket.packetId === INT(40293.toShort))
    assert(connectPacket.topic === STRING("test/topic"))
    assert(connectPacket.payload.encode === PUBLISHPAYLOAD(Array(0x1.toByte, 0x2.toByte, 0x3.toByte, 0x4.toByte, 0x5.toByte)).encode)
  }

  test("encode/decode publish packet - empty payload") {
    val connectPacket = PUBLISHDecoder.decode(
      PUBLISH(
        false, INT(3),
        false,
        STRING("test/topic"),
        INT(40293.toShort),
        PUBLISHPAYLOAD(Array())
      ).encode
    )

    assert(connectPacket.dup == false)
    assert(connectPacket.qos == INT(3))
    assert(connectPacket.retain == false)
    assert(connectPacket.packetId === INT(40293.toShort))
    assert(connectPacket.topic === STRING("test/topic"))
    assert(connectPacket.payload.encode === PUBLISHPAYLOAD(Array()).encode)
  }

  test("create puback packet") {
    val connectPacket = PUBACK(INT(40293.toShort))
    assert(connectPacket.packetId === INT(40293.toShort))

  }


  test("encode/decode puback packet") {
    val connectPacket = PUBACKDecoder.decode(PUBACK(INT(40293.toShort)).encode)
    assert(connectPacket.packetId === INT(40293.toShort))

  }


  test("create pubrec packet") {
    val connectPacket = PUBREC(INT(40293.toShort))
    assert(connectPacket.packetId === INT(40293.toShort))

  }


  test("encode/decode pubrec packet") {
    val connectPacket = PUBRECDecoder.decode(PUBREC(INT(40293.toShort)).encode)
    assert(connectPacket.packetId === INT(40293.toShort))

  }


  test("create pubrel packet") {
    val connectPacket = PUBREL(INT(40293.toShort))
    assert(connectPacket.packetId === INT(40293.toShort))

  }


  test("encode/decode pubrel packet") {
    val connectPacket = PUBRELDecoder.decode(PUBREL(INT(40293.toShort)).encode)
    assert(connectPacket.packetId === INT(40293.toShort))

  }

  test("create pubcomb packet") {
    val connectPacket = PUBCOMB(INT(40293.toShort))
    assert(connectPacket.packetId === INT(40293.toShort))

  }


  test("encode/decode pubcomb packet") {
    val connectPacket = PUBCOMBDecoder.decode(PUBCOMB(INT(40293.toShort)).encode)
    assert(connectPacket.packetId === INT(40293.toShort))

  }

}
