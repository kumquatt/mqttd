package plantae.citrus.mqtt.dto

/**
 * Created by yinjae on 15. 4. 17..
 */
case class CONNECT(clientId: STRING, cleanSession: Boolean, will: Option[Will], authentication: Option[Authentication],
                   keepAlive: INT) extends Packet {


  override def fixedHeader: FixedHeader = FixedHeader(ControlPacketType.CONNECT,
    List(ControlPacketFlag.CONNECT),
    REMAININGLENGTH(variableHeader.usedByte + payload.usedByte))

  override def variableHeader: VariableHeader = {
    def cleanSessionFlag: BYTE = {
      if (cleanSession) BYTE((0x01 << 1).toByte)
      else BYTE(0x00)
    }

    val willFlag = will match {
      case Some(it) => {
        def retain: BYTE = if (it.retain) BYTE((0x01 << 5).toByte) else BYTE(0x00)

        BYTE((0x01 << 2).toByte) | it.qos | retain
      }
      case None => BYTE(0x00)
    }


    val authenticationFlag = authentication match {
      case Some(it) => {
        val passwordFlag = it.password match {
          case Some(it) => BYTE((0x01 << 6).toByte)
          case None => BYTE(0x00)
        }
        passwordFlag | BYTE((0x01 << 7).toByte)
      }
      case None => BYTE(0x00)
    }


    VariableHeader(List(STRING("MQTT"), BYTE(0x4), (cleanSessionFlag | willFlag | authenticationFlag), keepAlive))
  }

  override def payload: Payload = {
    val willPayload = will match {
      case Some(it) => {
        List(it.topic, it.message)
      }
      case None => List()
    }

    val authenticationPayload = authentication match {
      case Some(it) => {
        val passwordPayload = it.password match {
          case Some(it) => List(it)
          case None => List()
        }
        List(it.id) ++ passwordPayload
      }
      case None => List()
    }


    Payload(List(clientId) ++ willPayload ++ authenticationPayload)
  }

  override val usedByte: Int = encode.length
}


case class Will(qos: BYTE, retain: Boolean, topic: STRING, message: STRING)

object WillQos {
  val QOS_1 = BYTE((0x00 << 3).toByte)
  val QOS_2 = BYTE((0x01 << 3).toByte)
  val QOS_3 = BYTE((0x02 << 3).toByte)
}

case class Authentication(id: STRING, password: Option[STRING])

object CONNECTDecoder {
  val WILL_QOS = BYTE((0x3 << 3).toByte)
  val CLEAN_SESSION = BYTE((0x1 << 1).toByte)
  val WILL_FLAG = BYTE((0x1 << 2).toByte)
  val WILL_RETAIN = BYTE((0x1 << 5).toByte)
  val AUTHENTICATION_PASSWORD = BYTE((0x1 << 6).toByte)
  val AUTHENTICATION_ID = BYTE((0x1 << 7).toByte)

  def decodeCONNECT(bytes: Array[Byte]): CONNECT = {
    val byteChunk = ByteChunkSplitter.split(bytes)

    val packetTypeAndFlag = Decoder.decodeBYTE(bytes)
    val remainingLength = Decoder.decodeREMAININGLENGTH(bytes.slice(1, bytes.length))


    val protocolName = Decoder.decodeSTRING(byteChunk.variableHeaderBytes)
    val protocolLevel = Decoder.decodeBYTE(byteChunk.variableHeaderBytes.slice(protocolName.usedByte, byteChunk.variableHeaderBytes.length))
    val flag = Decoder.decodeBYTE(byteChunk.variableHeaderBytes.slice(protocolLevel.usedByte + protocolName.usedByte, byteChunk.variableHeaderBytes.length))
    val keepAlive = Decoder.decodeINT(byteChunk.variableHeaderBytes.slice(protocolLevel.usedByte + protocolName.usedByte + flag.usedByte, byteChunk.variableHeaderBytes.length))
    val willFlag = flag & WILL_FLAG

    val willQos = flag & WILL_QOS
    val cleanSession = flag & CLEAN_SESSION
    val willRetain = flag & WILL_RETAIN
    val passwordFlag = flag & AUTHENTICATION_PASSWORD
    val idFlag = flag & AUTHENTICATION_ID

    val clientId = Decoder.decodeSTRING(byteChunk.payloadBytes)


    val willTopic = {
      if (willFlag.toBoolean) Decoder.decodeSTRING(byteChunk.payloadBytes.slice(clientId.usedByte, byteChunk.payloadBytes.length))
      else STRING(null)
    }

    val willMessage = {
      if (willFlag.toBoolean) Decoder.decodeSTRING(byteChunk.payloadBytes.slice(clientId.usedByte + willTopic.usedByte, byteChunk.payloadBytes.length))
      else STRING(null)
    }

    val id = {
      if (idFlag.toBoolean) Decoder.decodeSTRING(byteChunk.payloadBytes.slice(clientId.usedByte + willTopic.usedByte + willMessage.usedByte, byteChunk.payloadBytes.length))
      else STRING(null)
    }

    val password = {
      if (passwordFlag.toBoolean) Decoder.decodeSTRING(byteChunk.payloadBytes.slice(clientId.usedByte + willTopic.usedByte + willMessage.usedByte + id.usedByte, byteChunk.payloadBytes.length))
      else STRING(null)
    }

    CONNECT(clientId, cleanSession.value > 0, {
      if (willFlag.toBoolean) Option(Will(willQos, willRetain.value > 0, willTopic, willMessage))
      else Option(null)
    }, {
      if (idFlag.toBoolean) Option(Authentication(id, {
        if (passwordFlag.toBoolean) Option(password)
        else Option(null)
      }))
      else Option(null)
    }, keepAlive)

  }
}
