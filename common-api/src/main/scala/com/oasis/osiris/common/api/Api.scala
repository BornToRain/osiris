package com.oasis.osiris.common.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import com.lightbend.lagom.scaladsl.api.transport.Method

/**
  * 公用接口定义
  */
trait CommonService extends Service
{
  def bindCallUpRecord: ServiceCall[MoorDTO.Binding, Done]
  def hangUpCallUpRecord(id: String): ServiceCall[NotUsed, Done]
  def calledCallUpRecord(mobile: String): ServiceCall[NotUsed, String]
  def callbackCallUpRecord: ServiceCall[NotUsed, Done]
  def sms: ServiceCall[SmsDTO.Send, Done]
  def smsValidation: ServiceCall[SmsDTO.Validation, Boolean]
  def smsSuccess: ServiceCall[NotUsed, Done]
  def smsFail: ServiceCall[NotUsed, Done]
  def smsReply: ServiceCall[NotUsed, Done]

  import Service._

  def descriptor = named("commons").withCalls(
    //临时绑定电话关系
    restCall(Method.POST, "/commons/7moor/binding", bindCallUpRecord),
    //电话挂断
    restCall(Method.DELETE, "/commons/7moor/hang-up/:id", hangUpCallUpRecord _),
    //容联七陌回调 A=>坐席=>接口=>B
    restCall(Method.GET, "/commons/7moor?mobile", calledCallUpRecord _),
    //容联七陌通话事件推送更新回调
    restCall(Method.GET, "/commons/7moor/hang-up", callbackCallUpRecord),
    //发送验证码
    restCall(Method.POST, "/commons/sms", sms),
    //验证验证码
    restCall(Method.POST, "/commons/sms/validation", smsValidation),
    //短信发送成功
    restCall(Method.POST, "/commons/sms/success", smsSuccess),
    //短信发送失败
    restCall(Method.POST, "/commons/sms/fail", smsFail),
    //短信回复
    restCall(Method.POST, "/commons/sms/reply", smsReply)
  ).withAutoAcl(true)
}
