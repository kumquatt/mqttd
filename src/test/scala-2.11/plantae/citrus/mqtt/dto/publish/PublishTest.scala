
package plantae.citrus.mqtt.dto.publish

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import plantae.citrus.mqtt.dto.{INT, PUBLISHPAYLOAD, STRING}

@RunWith(classOf[JUnitRunner])
class PublishTest extends FunSuite {


  test("create publish packet") {
    val publish =
      PUBLISH(
        false, INT(1),
        false,
        STRING("test/topic"),
        Some(INT(40293.toShort)),
        PUBLISHPAYLOAD(Array(0x1.toByte, 0x2.toByte, 0x3.toByte, 0x4.toByte, 0x5.toByte))
      )

    assert(publish.dup == false)
    assert(publish.qos == INT(1))
    assert(publish.retain == false)
    assert(publish.packetId === Some(INT(40293.toShort)))
    assert(publish.topic === STRING("test/topic"))
    assert(publish.payload.encode === PUBLISHPAYLOAD(Array(0x1.toByte, 0x2.toByte, 0x3.toByte, 0x4.toByte, 0x5.toByte)).encode)
  }


  test("create publish packet - retain & dup") {
    val publish =
      PUBLISH(
        true, INT(2),
        true,
        STRING("test/topic"),
        Some(INT(40293.toShort)),
        PUBLISHPAYLOAD(Array(0x1.toByte, 0x2.toByte, 0x3.toByte, 0x4.toByte, 0x5.toByte))
      )

    assert(publish.dup == true)
    assert(publish.qos == INT(2))
    assert(publish.retain == true)
    assert(publish.packetId === Some(INT(40293.toShort)))
    assert(publish.topic === STRING("test/topic"))
    assert(publish.payload.encode === PUBLISHPAYLOAD(Array(0x1.toByte, 0x2.toByte, 0x3.toByte, 0x4.toByte, 0x5.toByte)).encode)
  }

  test("create publish packet - empty payload") {
    val publish =
      PUBLISH(
        false, INT(0),
        false,
        STRING("test/topic"),
        None,
        PUBLISHPAYLOAD("create publish packet - empty payload".getBytes)
      )

    assert(publish.dup == false)
    assert(publish.qos == INT(0))
    assert(publish.retain == false)
    assert(publish.packetId === None)
    assert(publish.topic === STRING("test/topic"))
    assert(new String(publish.payload.encode) === "create publish packet - empty payload")
  }


  test("encode/decode publish packet") {
    val publish = PUBLISHDecoder.decode(
      PUBLISH(
        false, INT(0),
        false,
        STRING("test/topic"),
        None,
        PUBLISHPAYLOAD(Array(0x1.toByte, 0x2.toByte, 0x3.toByte, 0x4.toByte, 0x5.toByte))
      ).encode
    )

    assert(publish.dup == false)
    assert(publish.qos == INT(0))
    assert(publish.retain == false)
    assert(publish.packetId === None)
    assert(publish.topic === STRING("test/topic"))
    assert(publish.payload.encode === PUBLISHPAYLOAD(Array(0x1.toByte, 0x2.toByte, 0x3.toByte, 0x4.toByte, 0x5.toByte)).encode)
  }


  test("encode/decode publish packet - retain & dup") {
    val publish = PUBLISHDecoder.decode(PUBLISH(
      true, INT(2),
      true,
      STRING("test/topic"),
      Some(INT(40293.toShort)),
      PUBLISHPAYLOAD(Array(0x1.toByte, 0x2.toByte, 0x3.toByte, 0x4.toByte, 0x5.toByte))
    ).encode
    )

    assert(publish.dup == true)
    assert(publish.qos == INT(2))
    assert(publish.retain == true)
    assert(publish.packetId === Some(INT(40293.toShort)))
    assert(publish.topic === STRING("test/topic"))
    assert(publish.payload.encode === PUBLISHPAYLOAD(Array(0x1.toByte, 0x2.toByte, 0x3.toByte, 0x4.toByte, 0x5.toByte)).encode)
  }

  test("encode/decode publish packet - empty payload") {
    val publish = PUBLISHDecoder.decode(
      PUBLISH(
        false, INT(3),
        false,
        STRING("test/topic"),
        Some(INT(40293.toShort)),
        PUBLISHPAYLOAD(Array())
      ).encode
    )

    assert(publish.dup == false)
    assert(publish.qos == INT(3))
    assert(publish.retain == false)
    assert(publish.packetId === Some(INT(40293.toShort)))
    assert(publish.topic === STRING("test/topic"))
    assert(publish.payload.encode === PUBLISHPAYLOAD(Array()).encode)
  }

  test("create puback packet") {
    val puback = PUBACK(INT(40293.toShort))
    assert(puback.packetId === INT(40293.toShort))
  }


  test("encode/decode puback packet") {
    val puback = PUBACKDecoder.decode(PUBACK(INT(40293.toShort)).encode)
    assert(puback.packetId === INT(40293.toShort))
  }


  test("create pubrec packet") {
    val pubrec = PUBREC(INT(40293.toShort))
    assert(pubrec.packetId === INT(40293.toShort))
  }


  test("encode/decode pubrec packet") {
    val pubrec = PUBRECDecoder.decode(PUBREC(INT(40293.toShort)).encode)
    assert(pubrec.packetId === INT(40293.toShort))
  }


  test("create pubrel packet") {
    val pubrel = PUBREL(INT(40293.toShort))
    assert(pubrel.packetId === INT(40293.toShort))
  }


  test("encode/decode pubrel packet") {
    val pubrel = PUBRELDecoder.decode(PUBREL(INT(40293.toShort)).encode)
    assert(pubrel.packetId === INT(40293.toShort))
  }

  test("create pubcomb packet") {
    val pubcomb = PUBCOMB(INT(40293.toShort))
    assert(pubcomb.packetId === INT(40293.toShort))
  }


  test("encode/decode pubcomb packet") {
    val pubcomb = PUBCOMBDecoder.decode(PUBCOMB(INT(40293.toShort)).encode)
    assert(pubcomb.packetId === INT(40293.toShort))
  }

}
