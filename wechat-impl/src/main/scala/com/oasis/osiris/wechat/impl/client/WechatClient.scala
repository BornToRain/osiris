package com.oasis.osiris.wechat.impl.client
import akka.actor.ActorSystem
import akka.event.slf4j.Slf4jLogger
import com.oasis.osiris.tool.functional.Lift.ops._
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext

/**
  * 微信客户端
  */
class WechatClient
(
  ws                 : WSClient,
  redis              : RedisClient,
  actorSystem        : ActorSystem
)(implicit ec                               : ExecutionContext) extends Slf4jLogger
{
  import play.api.http.{ContentTypes, HeaderNames}
  import play.api.libs.json.{Json, Writes}

  implicit           val qs               = Array("access_token=#")
  private[this] lazy val redisAccessToken = "AccessToken"
  private[this] lazy val redisJsApiTicket = "JsApiTicket"
  private[this] lazy val redisApiTicket   = "ApiTicket"

  /**
    * Get请求 封装好AccessToken
    */
  private[this] def get(uri: String)(qs: String*) = for
  {
    token <- accessToken
    queryString <- qs.map(_.split("=")).map
    {
      case Array(x, y) => (x, if (x == "access_token") y
      else token)
    }.liftF
    get <- ws.url(s"${WechatClient.gateway }$uri")
    .withQueryString(queryString: _*)
    .get()
  } yield get

  /**
    * Post请求 封装好AccessToken
    */
  private[this] def post[T](uri: String)(data: T)(implicit w: Writes[T]) = for
  {
    token <- accessToken
    queryString <- qs.map(_.split("=")).map
    {
      case Array(x, y) => (x, if (x == "access_token") y
      else token)
    }.liftF
    post <- ws.url(s"${WechatClient.gateway }$uri")
    .withQueryString(queryString: _*)
    .withHeaders(HeaderNames.CONTENT_TYPE -> ContentTypes.JSON, HeaderNames.ACCEPT_CHARSET -> "UTF-8")
    .post(Json.toJson(data))
  } yield post

  //微信AccessToken
  val accessToken = for
  {
    redis <- RedisClient(actorSystem).client.liftF
    //缓存中Token
    cache <- redis.get[String](redisAccessToken)
    //有效token
    token <- cache match
    {
      //缓存获取
      case Some(d) => d.liftF
      //缓存失效 网络请求获取
      case _ => for
      {
        response <- get("cgi-bin/token")("grant_type=client_credential", s"appid=${WechatClient.appId }", s"secret=${WechatClient.secret }")
        web <-
        {
          val body = response.json
          log.info(s"微信JsApiTicket响应 =====> $body")
          (body \ "ticket").as[String]
        }.liftF
        //存储一个小时
        _ <- redis.set(redisJsApiTicket, web, Some(3600L))
      } yield web
    }
  } yield token
  //微信JsApiTicket
  val jsApiTicket = for
  {
    redis <- RedisClient(actorSystem).client.liftF
    //缓存中JsApiTicket
    cache <- redis.get[String](redisJsApiTicket)
    //有效JsApiTicket
    jsApiTicket <- cache match
    {
      //缓存获取
      case Some(d) => d.liftF
      //缓存失效 网络请求获取
      case _ => for
      {
        response <- get("cgi-bin/ticket/getticket")("access_token=#", "type=jsapi")
        web <-
        {
          val body = response.json
          log.info(s"微信JsApiTicket响应 =====> $body")
          (body \ "ticket").as[String]
        }.liftF
        //存储一个小时
        _ <- redis.set(redisJsApiTicket, web, Some(3600L))
      } yield web
    }
  } yield jsApiTicket
  //微信ApiTicket
  val apiTicket   = for
  {
    redis <- RedisClient(actorSystem).client.liftF
    //缓存中ApiTicket
    cache <- redis.get[String](redisApiTicket)
    //有效ApiTicket
    apiTicket <- cache match
    {
      //缓存获取
      case Some(d) => d.liftF
      //缓存失效 网络请求获取
      case _ => for
      {
        response <- get("cgi-bin/ticket/getticket")("access_token=#", "type=wx_card")
        web <-
        {
          val body = response.json
          log.info(s"微信ApiTicket响应 =====> $body")
          (body \ "ticket").as[String]
        }.liftF
        //存储一个小时
        _ <- redis.set(redisApiTicket, web, Some(3600L))
      } yield web
    }
  } yield apiTicket

  import com.oasis.osiris.tool.EncryptionTool

  import scala.concurrent.duration._

  /**
    * 运行微信公众号客户端
    * 5秒延迟后开始运行
    * 每一个小时刷新一次
    */
  def startWechat = actorSystem.scheduler.schedule(5.second, 1.hour)
  {
    _ =>
    accessToken.onSuccess
    {
      case _ => if (WechatClient.enableJsApiTicket) jsApiTicket
      if (WechatClient.enableApiTicket) apiTicket
    }
  }

  /**
    * 获取Oauth2
    */
  def oauth2(code: String) = get("sns/oauth2/access_token")(s"appid=${WechatClient.appId }", s"secret=${WechatClient.secret }", s"code=$code",
    "grant_type=authorization_code")
  .map(_.json)

  /**
    * js-sdk签名
    */
  def jsSign(timeStamp: String)(nonceStr: String)(uri: String) = for
  {
    ticket <- jsApiTicket
    str <- s"jsapi_ticket=$ticket&noncestr=$nonceStr&timestamp=$timeStamp&url=$uri".liftF
    sign <- EncryptionTool.SHA1(str).liftF
  } yield sign

  /**
    * 校验微信服务器签名
    */
  def check(signature: String)(timeStamp: String)(nonce: String) = for
  {
    str <- Array(WechatClient.token, timeStamp, nonce)
    .sorted
    .reduce(_ + _).liftF
    sign <- EncryptionTool.SHA1(str).liftF
    b <- sign.equalsIgnoreCase(signature).liftF
  } yield b
}

object WechatClient
{
  import com.typesafe.config.ConfigFactory
  private[this]               val config            = ConfigFactory.load
  private[WechatClient$]      val appId             = config.getString("app-id")
  private[WechatClient$]      val secret            = config.getString("secret")
  private[WechatClient$]      val mchId             = config.getString("mch-id")
  private[WechatClient$]      val key               = config.getString("key")
  private[WechatClient$]      val token             = config.getString("token")
  private[WechatClient$]      val enableJsApiTicket = config.getBoolean("enable-js-api-ticket")
  private[WechatClient$]      val enableApiTicket   = config.getBoolean("enable-api-ticket")
  private[WechatClient$] lazy val certPath          = config.getString("cert-path")
  private[WechatClient$] lazy val certPwd           = config.getString("cert-password")
  private[WechatClient$]      val gateway           = "https://api.weixin.qq.com/"
}

