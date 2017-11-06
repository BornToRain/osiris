package com.oasis.osiris.wechat.api

import com.lightbend.lagom.scaladsl.api.Service

/**
  * 微信接口定义
  */
trait WechatService extends Service
{
  import akka.{Done, NotUsed}
  import com.lightbend.lagom.scaladsl.api.ServiceCall

  def get(signature: String, timestamp: String, nonce: String, echostr: String): ServiceCall[NotUsed, Done]
  def post: ServiceCall[NotUsed, String]

  import Service._
  import com.lightbend.lagom.scaladsl.api.transport.Method

  def descriptor = named("wechat").withCalls(
    //确认消息来自微信服务器
    restCall(Method.GET, "/wechat?signature&timestamp&nonce&echostr", get _),
    //处理微信服务器发来的消息(XML格式)
    restCall(Method.POST, "/wechat", post)
  ).withAutoAcl(true)
}
