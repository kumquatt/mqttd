package plantae.citrus.exercise

import scodec.bits._

object Scodec extends App{


  val otp = hex"54686973206973206e6f74206120676f6f642070616420746f2075736521".bits
  // otp: scodec.bits.BitVector = BitVector(240 bits, 0x54686973206973206e6f74206120676f6f642070616420746f2075736521)

  val bits = hex"746be39ece241e0da28b7acd4fad63632249ec5e2e402d5a0b2cd95d0a05".bits
  // bits: scodec.bits.BitVector = BitVector(240 bits, 0x746be39ece241e0da28b7acd4fad63632249ec5e2e402d5a0b2cd95d0a05)

  val decoded = (bits ^ otp) rotateLeft 3
  // decoded: scodec.bits.BitVector = BitVector(240 bits, 0x001c576f726b696e6720776974682062696e617279206973206561737921)

  println("hello world")

  //  val msg = variableSizeBytes(uint16, utf8).decode(decoded).require.value
  //  //msg: String = Working with binary is easy!

}
