package com.oasis.osiris.wechat.api

import com.lightbend.lagom.scaladsl.api.Service

/**
  * 微信接口定义
  */
trait WechatService extends Service
{
  import akka.{Done, NotUsed}
  import com.lightbend.lagom.scaladsl.api.ServiceCall
  import play.api.libs.json.JsValue

  import scala.xml.Elem

  def createTag: ServiceCall[TagDTO.Create, Done]
  def createQRCode: ServiceCall[QRCodeDTO.Create, Done]
  def getOpenId(code: String): ServiceCall[NotUsed,JsValue]
  def getJsSDK(uri: String): ServiceCall[NotUsed,JsSDK]
  def resetMenu: ServiceCall[NotUsed,JsValue]
  def createMenu: ServiceCall[MenuDTO.Create,Done]
  def get(signature: String, timestamp: String, nonce: String, echostr: String): ServiceCall[NotUsed, String]
  def post: ServiceCall[Elem, String]

  import Service.{restCall, _}
  import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer
  import com.lightbend.lagom.scaladsl.api.transport.Method
  import com.oasis.osiris.tool.serialization.CustomMessageSerializer

  def descriptor = named("wechat").withCalls(
    //创建微信标签
    restCall(Method.POST, "/wechat/tags", createTag),
    //创建微信二维码
    restCall(Method.POST, "/wechat/qrcodes", createQRCode),
    //获取用户OpenId
    restCall(Method.GET, "/wechat/oauth2/openId?code", getOpenId _),
    //获取Js-SDK
    restCall(Method.GET, "/wechat/js-sdk?uri", getJsSDK _),
    //重置微信菜单
    restCall(Method.POST, "/wechat/menus/reset", resetMenu),
    //创建微信菜单
    restCall(Method.POST, "/wechat/menus/", createMenu),
    //确认消息来自微信服务器
    restCall(Method.GET, "/wechat?signature&timestamp&nonce&echostr", get _),
    //处理微信服务器发来的消息(XML格式)
    restCall(Method.POST, "/wechat", post)(CustomMessageSerializer.XMLMessageSerializer, MessageSerializer.StringMessageSerializer)
  ).withAutoAcl(true)
}
