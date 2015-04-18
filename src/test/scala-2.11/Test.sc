import plantae.citrus.mqtt.dto._
//
//new BYTE(0x80.toByte).isMostSignificantBitOn
//"will message will messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill message will messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill message".getBytes.length
//val longStringEncode = STRING("will message will messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill message will messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill message").encode.toArray
//val longStringDecode = Decoder.decodeSTRING(STRING("will message will messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill message will messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill message").encode.toArray)
//
//
//BYTE(0x70.toByte).isMostSignificantBitOn
//BYTE(0x80.toByte).isMostSignificantBitOn
//
//BYTE(1).isLeastSignificantBitOn
//BYTE(2).isMostSignificantBitOn
//BYTE(3).isMostSignificantBitOn
//
//
//INT(Short.MaxValue).leastSignificantByte.hexa
//INT(Short.MaxValue).mostSignificantByte.hexa
//
//INT(Short.MinValue).leastSignificantByte.hexa
//INT(Short.MinValue).mostSignificantByte.hexa
//
//STRING("client_id").encode
//"client_id".getBytes

//CONNECT(
//  STRING("client_id"), true,
//  Option(null),
//  Option(null),
//  INT(60)
//).encode
//
//CONNECT(
//  STRING("client_id"), true,
//  Option(Will(WillQos.QOS_1,true , STRING("testWillTopic") , STRING("TestWillMessage") )),
//  Option(Authentication(STRING("id"), Option(STRING("password")))),
//  INT(30000)
//).encode
//
//new String(
//  CONNECT(
//    STRING("client_id"), true,
//    Option(Will(WillQos.QOS_1,true , STRING("testWillTopic") , STRING("TestWillMessage") )),
//    Option(Authentication(STRING("id"), Option(STRING("password")))),
//    INT(30000)
//  ).encode
//)
//val bytes =CONNECT(
//  STRING("client_id"), true,
//  Option(Will(WillQos.QOS_1,true , STRING("testWillTopic") , STRING("will message will messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill message") )),
//  Option(Authentication(STRING("id"), Option(STRING("password")))),
//  INT(30000)
//).encode

//
//
//
//var all =CONNECTDecoder.decodeCONNECT(bytes)
//
//
//val min =CONNECTDecoder.decodeCONNECT(
//  CONNECT(
//  STRING("client_id"), true,
//  Option(null),
//  Option(null),
//  INT(60)
//).encode
//)
//
//val min_id = CONNECTDecoder.decodeCONNECT(
//  CONNECT(
//    STRING("client_id"), true,
//    Option(null),
//    Option(Authentication(STRING("id"),Option(null))),
//    INT(60)
//  ).encode
//)
//
//val min_pass =CONNECTDecoder.decodeCONNECT(
//  CONNECT(
//    STRING("client_id"), true,
//    Option(null),
//    Option(Authentication(STRING("id"),Option(STRING("password")))),
//    INT(60)
//  ).encode
//)
//
//val min_will = CONNECTDecoder.decodeCONNECT(
//  CONNECT(
//    STRING("client_id"), false,
//    Option(Will(WillQos.QOS_3, true, STRING("will topic topic") , STRING("will message will messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill message will messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill messagewill message"))),
//    Option(null),
//    INT(60)
//  ).encode
//)
