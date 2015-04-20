package plantae.citrus.mqtt.dto.ping

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import plantae.citrus.mqtt.dto.ControlPacketType

@RunWith(classOf[JUnitRunner])
class PingTest extends FunSuite{

  test("encode PingReq"){
    val ping = PINGREQ
    assert(ping.fixedHeader.packetType === ControlPacketType.PINGREQ)
  }

  test("decode PingReq"){
    assert(PINGREQDecoder.decode(PINGREQ.encode).fixedHeader.packetType === ControlPacketType.PINGREQ)
  }

  test("encode PingResp"){
    val ping = PINGRESP
    assert(ping.fixedHeader.packetType === ControlPacketType.PINGRESP)
  }

  test("decode PingResp"){
    assert(PINGRESPDecoder.decode(PINGRESP.encode).fixedHeader.packetType === ControlPacketType.PINGRESP)
  }
}
