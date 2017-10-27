package com.oasis.osiris.common.impl

import com.oasis.osiris.tool.{DateTool, EncryptionTool}
import com.oasis.osiris.tool.functional.Lift.ops._
import com.typesafe.config.ConfigFactory
import play.api.http.HeaderNames
import play.api.libs.json.{Format, Json, Writes}
import play.api.libs.ws._
import scala.concurrent.ExecutionContext

sealed trait MoorRequest

object MoorRequest
{

  //挂断请求
  case class HangUp(CallId: Option[String], Agent: Option[String], ActionID: String)

  object HangUp
  {
    implicit val format: Format[HangUp] = Json.format
  }

}

class MoorClient(ws: WSClient)(implicit ec: ExecutionContext)
{
  import com.oasis.osiris.common.impl.MoorRequest._
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
    post <- ws.url(s"${MoorClient.baseUri }$uri")
    .withQueryString("sig" -> sig)
    .withHeaders(HeaderNames.AUTHORIZATION -> auth)
    .post(Json.toJson(data))
  } yield post

  //挂断请求
  def hangUp(request: HangUp) = post(s"v20160818/call/hangup/${MoorClient.account }")(request)
}

object MoorClient
{
  lazy val account = ConfigFactory.load.getString("7moor.account")
  lazy val secret = ConfigFactory.load.getString("7moor.secret")
  lazy val baseUri = "http://apis.7moor.com/"
}
