package plantae.citrus.mqtt.dto

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DataFormatTest extends FunSuite {

  test("BYTE test") {
    assert(BYTE(0x80.toByte).isMostSignificantBitOn === true)
    assert(BYTE(0x70.toByte).isMostSignificantBitOn === false)
    assert(BYTE(0x80.toByte).isMostSignificantBitOn === true)

    assert(BYTE(1).isLeastSignificantBitOn === true)
    assert(BYTE(2).isMostSignificantBitOn === false)
    assert(BYTE(3).isMostSignificantBitOn === false)
    assert((BYTE(0x1) << 3) == BYTE(0x8))
    assert(BYTE(0x0).toBoolean == false)
    assert(Range(0x1, 0xFF).foldLeft(true)((a, b) => {
      a && BYTE(b.toByte).toBoolean
    }) == true)

  }

  test("INT test") {
    Range(-32768,32767).foreach(each => each == Decoder.decodeINT(Decoder.ByteStream(INT(each.toShort).encode.toArray)).value)
    println(Decoder.decodeINT(Decoder.ByteStream(INT(128).encode.toArray)))
    println(INT(-128).encode)

    assert(INT(Short.MaxValue).leastSignificantByte.hexa === "0xFF")
    assert(INT(Short.MaxValue).mostSignificantByte.hexa === "0x7F")

    assert(INT(Short.MinValue).leastSignificantByte.hexa === "0x0")
    assert(INT(Short.MinValue).mostSignificantByte.hexa === "0x80")

  }

  test("STRING test") {

    assert(STRING("client_id").encode === List(0, 9, 99, 108, 105, 101, 110, 116, 95, 105, 100))
    assert("client_id".getBytes === Array(99, 108, 105, 101, 110, 116, 95, 105, 100))

    val shortString = "aaa"
    val longString = "will message will messagewill messagewill messagewill messagewill messagewill messagewill " +
      "messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill message will " +
      "messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill " +
      "messagewill messagewill messagewill messagewill message"
    assert(Decoder.decodeSTRING(Decoder.ByteStream(STRING(shortString).encode.toArray)) === STRING(shortString))
    assert(Decoder.decodeSTRING(Decoder.ByteStream(STRING(longString).encode.toArray)) === STRING(longString))
  }

}
