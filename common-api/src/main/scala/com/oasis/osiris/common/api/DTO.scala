package com.oasis.osiris.common.api

import com.oasis.osiris.tool.JSONTool._
import play.api.libs.json.{Format, Json}

/**
  * 接口层DTO对象
  */
//容联七陌DTO
object MoorDTO
{

  //绑定关系
  case class Binding(thirdId: Option[String], call: String, called: String, maxCallTime: Option[Long], noticeUri: Option[String])

  object Binding
  {
    implicit val format: Format[Binding] = Json.format
  }

  //通话状态
  object CallStatus extends Enumeration
  {
    type CallStatus = Value
    //已接、振铃未接听、ivr放弃、排队放弃、黑名单、留言
    val dealing, notDeal, leak, queueLeak, blackList, voicemail = Value
    implicit val format: Format[CallStatus] = enumFormat(CallStatus)
  }

  //通话事件状态
  object CallEventStatus extends Enumeration
  {
    type CallEventStatus = Value
    val Ring, Ringing, Link, Hangup, Unlink = Value
    implicit val format: Format[CallEventStatus] = enumFormat(CallEventStatus)
  }

}

//短信
object SmsDTO
{

  case class SmsDTO(mobile: String, smsType: String, captcha)

  object SmsDTO
  {
    implicit val format: Format[SmsDTO] = Json.format
  }

}
