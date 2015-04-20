package plantae.citrus.mqtt.dto

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PacketTest extends FunSuite {

  test("Payload test") {
    assert(Payload(List()).encode === Array())
    assert(Payload(List(BYTE(0x01), INT(32))).encode === Array[Byte](0x01, 0x00, 0x20))
    assert(Payload(List(BYTE(0x01), INT(32), STRING("hello world"))).encode.size == 16)
    assert(Payload(List(BYTE(0x01), INT(32), STRING("hello world"), STRING("hello world2"))).encode.size == 30)
  }

  test("VariableHeader test") {
    assert(VariableHeader(List()).encode === Array())
    assert(VariableHeader(List(BYTE(0x01), INT(32))).encode === Array[Byte](0x01, 0x00, 0x20))
    assert(VariableHeader(List(BYTE(0x01), INT(32), STRING("hello world"))).encode.size == 16)
    assert(VariableHeader(List(BYTE(0x01), INT(32), STRING("hello world"), STRING("hello world2"))).encode.size == 30)
  }

  test("FixedHeader Test") {
    assert(FixedHeader(BYTE(0x00), REMAININGLENGTH(1)).encode === Array(0x00, 0x01))
  }
}
