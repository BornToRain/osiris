package com.oasis.osiris.common.api

import play.api.libs.json.{Format, Json}

/**
  * 接口层DTO对象
  */
//容联七陌DTO
object MoorDTO
{

  //绑定关系
  case class Binding(thirdId: Option[String], call: String, called: String, maxCallTime: Option[Int], noticeUri: Option[String])

  object Binding
  {
    implicit val format: Format[Binding] = Json.format
  }

}

//短信DTO
object SmsDTO
{
  import com.oasis.osiris.common.api.SmsDTO.SmsType.SmsType
  import com.oasis.osiris.tool.JSONTool.enumFormat

  //发送
  case class Send(mobile: String, smsType: SmsType)

  object Send
  {
    implicit val format: Format[Send] = Json.format
  }

  //验证
  case class Validation(mobile: String, smsType: SmsType, captcha: String)

  object Validation
  {
    implicit val format: Format[Validation] = Json.format
  }

  //短信类型
  object SmsType extends Enumeration
  {
    type SmsType = Value
    //登录、注册、邀请、交付、达人通知
    val login, register, invitation, payment, notice = Value
    implicit val format: Format[SmsType] = enumFormat(SmsType)
  }

}
