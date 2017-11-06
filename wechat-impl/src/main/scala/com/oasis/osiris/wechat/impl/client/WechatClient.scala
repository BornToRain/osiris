package com.oasis.osiris.wechat.impl.client
import com.oasis.osiris.tool.functional.Lift.ops._
import play.api.libs.ws.WSClient
import redis.RedisClient

import scala.concurrent.ExecutionContext

/**
  * 微信客户端
  */
class WechatClient
(
  ws   : WSClient,
  redis: RedisClient
)(implicit ec: ExecutionContext)
{
  import org.slf4j.LoggerFactory

  import scala.concurrent.Future

  private[this] val log = LoggerFactory.getLogger(classOf[WechatClient])
  private[this] val redisAccessToken = "AccessToken"
  private[this] val redisJsApiTicket = "JsApiTicket"
  private[this] val redisApiTicket   = "ApiTicket"

  /**
    * Get请求 封装好AccessToken
    */
  def get(uri: String)(qs: String*) = for
  {
    queryString <- qs.map(_.split("=")) match
    {
      case a:Seq[Array[String]] => a.map
      {
        case Array(x,y) => (x,y)
//        case Array(x,_) if x == "access_token" => accessToken.map((x,_))
      }.liftF
      // if x == "access_token" => accessToken.map(d => (x, d))
//      case Array(x,y,z)                        =>
//        println(s"x$x")
//      Array((x, "1")).liftF
    }
    get <- ws.url(s"${WechatClient.gateway }$uri")
    .withQueryString(queryString: _*)
    .get
  } yield get

  private[this] def replaceToken(qs:String) = accessToken.map(s => qs.replace("",s))

  /**
    * Post请求 封装好AccessToken
    */
//  private[this] def post[T](uri: String)(data: T)(implicit w: Writes[T]) = for
//  {
//    token <- accessToken
//    queryString <- qs.map(_.split("=")).map
//    {
//      case Array(x, y) => (x, if (x == "access_token") y
//      else token)
//    }.liftF
//    post <- ws.url(s"${WechatClient.gateway }$uri")
//    .withQueryString(queryString: _*)
//    .withHeaders(HeaderNames.CONTENT_TYPE -> ContentTypes.JSON, HeaderNames.ACCEPT_CHARSET -> "UTF-8")
//    .post(Json.toJson(data))
//  } yield post

  //微信AccessToken
  def accessToken: Future[String] = for
  {
    //缓存中Token
    cache <- redis.get[String](redisAccessToken)
    //有效token
    token <-
    {
      cache match
      {
        //缓存获取
        case Some(d) => d.liftF
        //缓存失效 网络请求获取
        case _       => for
        {
          response <- get("cgi-bin/token")("grant_type=client_credential", s"appid=${WechatClient.appId }", s"secret=${WechatClient.secret }")
          web <-
          {
            val body = response.json
            log.info(s"微信AccessToken响应 =====> $body")
            (body \ "ticket").as[String]
          }.liftF
          //存储一个小时
          _ <- redis.set(redisAccessToken, web, Some(3600L))
        } yield web
      }
    }
  } yield token
  //微信JsApiTicket
  def jsApiTicket: Future[String] = for
  {
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
  def apiTicket: Future[String] = for
  {
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

  /**
    * 运行微信公众号客户端
    * 5秒延迟后开始运行
    * 每一个小时刷新一次
    */
//  def startWechat = actorSystem.scheduler.schedule(5.second, 1.hour)
//  {
//    accessToken.onSuccess
//    {
//      case _ => if (WechatClient.enableJsApiTicket) jsApiTicket
//      if (WechatClient.enableApiTicket) apiTicket
//    }
//  }

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
    result <- sign.equalsIgnoreCase(signature).liftF
  } yield result
}

object WechatClient
{
  import com.typesafe.config.ConfigFactory

  private[this]        val config            = ConfigFactory.load
  private[client]      val appId             = config.getString("wechat.app-id")
  private[client]      val secret            = config.getString("wechat.secret")
  //  private[client] val mchId             = config.getString("mch-id")
//  private[client] val key               = config.getString("key")
  private[client]      val token             = config.getString("wechat.token")
  private[client]      val enableJsApiTicket = config.getBoolean("wechat.enable-js-api-ticket")
  private[client]      val enableApiTicket   = config.getBoolean("wechat.enable-api-ticket")
  private[client] lazy val certPath          = config.getString("wechat.cert-path")
  private[client] lazy val certPwd           = config.getString("wechat.cert-password")
  private[client]      val gateway           = "https://api.weixin.qq.com/"
}
