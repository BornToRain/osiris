package com.oasis.osiris.wechat.api

import com.lightbend.lagom.scaladsl.api.Service

/**
  * 微信接口定义
  */
trait WechatService extends Service
{
  import akka.{Done, NotUsed}
  import com.lightbend.lagom.scaladsl.api.ServiceCall

  import scala.xml.Elem

  def get(signature: String, timestamp: String, nonce: String, echostr: String): ServiceCall[NotUsed, String]
  def post: ServiceCall[Elem, String]
  def createTag: ServiceCall[TagDTO.Create, Done]

  import Service._
  import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer
  import com.lightbend.lagom.scaladsl.api.transport.Method
  import com.oasis.osiris.tool.serialization.CustomMessageSerializer

  def descriptor = named("wechat").withCalls(
    //确认消息来自微信服务器
    restCall(Method.GET, "/wechat?signature&timestamp&nonce&echostr", get _),
    //处理微信服务器发来的消息(XML格式)
    restCall(Method.POST, "/wechat", post)(CustomMessageSerializer.XMLMessageSerializer, MessageSerializer.StringMessageSerializer),
    //创建微信标签
    restCall(Method.POST, "/wechat/tags", createTag)
  ).withAutoAcl(true)
}
