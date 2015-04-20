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
      if (cleanSession) BYTE(0x01) << 1
      else BYTE(0x00)
    }

    val willFlag = will match {
      case Some(it) => {
        def retain: BYTE = if (it.retain) BYTE((0x01 << 5).toByte) else BYTE(0x00)

        BYTE(0x01) << 2 | it.qos | retain
      }
      case None => BYTE(0x00)
    }


    val authenticationFlag = authentication match {
      case Some(it) => {
        val passwordFlag = it.password match {
          case Some(it) => BYTE(0x01) << 6
          case None => BYTE(0x00)
        }
        passwordFlag | BYTE(0x01) << 7
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

// TODO : check belows.
// how about extends enumeration ? or case class
case object WillQos {
  val QOS_1 = BYTE(0x00) << 3
  val QOS_2 = BYTE(0x01) << 3
  val QOS_3 = BYTE(0x02) << 3
}

// TODO : check belows
case class WillQos2(level: Int) {
  def getValue = {
    level match {
      case 1 => BYTE(0x00) << 3
      case 2 => BYTE(0x01) << 3
      case 3 => BYTE(0x03) << 3
      case _ => BYTE(0x00) << 3 // default value is Qos 1
    }
  }
}

case class Authentication(id: STRING, password: Option[STRING])

object CONNECTDecoder {
  val CLEAN_SESSION = BYTE(0x1) << 1
  val WILL_FLAG = BYTE(0x1) << 2
  val WILL_QOS = BYTE(0x3) << 3
  val WILL_RETAIN = BYTE(0x1) << 5
  val AUTHENTICATION_PASSWORD = BYTE(0x1) << 6
  val AUTHENTICATION_ID = BYTE(0x1) << 7

  def decodeCONNECT(bytes: Array[Byte]): CONNECT = {
    val streammer = Decoder.ByteStreammer(bytes)
    val packetTypeAndFlag = Decoder.decodeBYTE(streammer)

    val remainingLength = Decoder.decodeREMAININGLENGTH(streammer)

    val protocolName = Decoder.decodeSTRING(streammer)
    val protocolLevel = Decoder.decodeBYTE(streammer)
    val flag = Decoder.decodeBYTE(streammer)
    val keepAlive = Decoder.decodeINT(streammer)
    val willFlag = flag & WILL_FLAG

    val cleanSession = flag & CLEAN_SESSION
    val passwordFlag = flag & AUTHENTICATION_PASSWORD
    val idFlag = flag & AUTHENTICATION_ID

    val clientId = Decoder.decodeSTRING(streammer)


    val will = {
      if (willFlag.toBoolean) {
        Some(Will(flag & WILL_QOS,
          (flag & WILL_RETAIN).toBoolean,
          Decoder.decodeSTRING(streammer),
          Decoder.decodeSTRING(streammer))
        )
      } else {
        None
      }
    }


    val authentication = {
      if (idFlag.toBoolean) Some(Authentication(Decoder.decodeSTRING(streammer), {
        if (passwordFlag.toBoolean) {
          Some(Decoder.decodeSTRING(streammer))
        } else {
          None
        }
      }
      )
      )
      else None
    }

    CONNECT(clientId, cleanSession.toBoolean, will, authentication, keepAlive)

  }
}
