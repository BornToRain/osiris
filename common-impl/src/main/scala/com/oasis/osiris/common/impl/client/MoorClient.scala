package com.oasis.osiris.common.impl.client

import com.oasis.osiris.tool.{DateTool, EncryptionTool}
import com.oasis.osiris.tool.functional.Lift.ops._
import play.api.http.HeaderNames
import play.api.libs.json.{Format, Json, Writes}
import play.api.libs.ws._

import scala.concurrent.ExecutionContext

object MoorRequest
{

  //挂断请求
  case class HangUp(CallId: Option[String], Agent: Option[String], ActionID: String)

  object HangUp
  {
    implicit val format: Format[HangUp] = Json.format
  }

}

/**
  * 容联七陌客户端
  */
class MoorClient(ws: WSClient)(implicit ec: ExecutionContext)
{
  import MoorRequest._
  import play.api.libs.json.Json

  /**
    * 容联七陌鉴权 请求头部分
    * Base64编码(账户Id+冒号+时间戳)
    */
  private[this] def authenticationHeader(timeStamp: String) = EncryptionTool.base64(s"${MoorClient.account }:$timeStamp")

  /**
    * 容联七陌鉴权 请求参数部分
    * MD5编码(帐号Id+帐号APISecret+时间戳)
    * 转大写
    */
  private[this] def authenticationParameter(timeStamp: String) = EncryptionTool.md5(s"${MoorClient.account }${MoorClient.secret }$timeStamp")
  .toUpperCase

  /**
    * 容联七陌客户端POST
    * 已拼接好鉴权部分
    */
  private[this] def post[T](uri: String)(data: T)(implicit w: Writes[T]) = for
  {
    timeStamp <- DateTool.datetimeStamp.liftF
    auth <- authenticationHeader(timeStamp).liftF
    sig <- authenticationParameter(timeStamp).liftF
    post <- ws.url(s"${MoorClient.gateway }$uri")
    .withQueryString("sig" -> sig)
    .withHeaders(HeaderNames.AUTHORIZATION -> auth)
    .post(Json.toJson(data))
  } yield post

  //挂断请求
  def hangUp(request: HangUp) = post(s"v20160818/call/hangup/${MoorClient.account }")(request)
}

object MoorClient
{
  import com.typesafe.config.ConfigFactory
  private[this]   val config  = ConfigFactory.load
  //容联七陌账号
  private[client] val account = config.getString("7moor.account")
  //容联七陌密钥
  private[client] val secret  = config.getString("7moor.secret")
  //容联七陌网关
  private[client] val gateway = "http://apis.7moor.com/"
}
