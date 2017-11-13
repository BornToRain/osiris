package com.oasis.osiris.wechat.impl.client
import akka.actor.ActorSystem
import com.oasis.osiris.tool.functional.Lift.ops._
import play.api.libs.ws.WSClient
import redis.RedisClient

import scala.concurrent.ExecutionContext

/**
  * 微信客户端
  */
class WechatClient
(
  ws         : WSClient,
  redis      : RedisClient,
  actorSystem: ActorSystem
)(implicit ec: ExecutionContext)
{
  import org.slf4j.LoggerFactory
  import play.api.http.{ContentTypes, HeaderNames}
  import play.api.libs.json.{Json, Writes}

  import scala.concurrent.Future

  private[this]    val log                  = LoggerFactory.getLogger(classOf[WechatClient])
  private[this]    val redisAccessToken     = "AccessToken"
  private[this]    val redisJsApiTicket     = "JsApiTicket"
  private[this]    val redisApiTicket       = "ApiTicket"

  /**
    * 替换AccessToken值
    */
  private[this] def replaceAccessToken(qs: (String, String)*) = qs.contains(("access_token", "#")) match
  {
    case true => accessToken.map
    {
      d =>
      qs.foldLeft(Seq.empty[(String, String)])
      {
        case (xs, (x@"access_token", _)) => xs :+ (x, d)
        case (xs, t)                     => xs :+ t
      }
    }
    case _    => qs.liftF
  }

  /**
    * Get请求 封装好AccessToken
    */
  private[this] def get(uri: String)(qs: (String, String)*) = for
  {
    queryString <- replaceAccessToken(qs:_*)
    get         <- ws.url(s"${WechatClient.gateway }$uri")
    .withQueryString(queryString: _*)
    .get
  } yield get

  /**
    * Post请求 封装好AccessToken
    */
  private[this] def post[T](uri: String)(data: T)(qs: (String, String)*)(implicit w: Writes[T]) = for
  {
    queryString <- replaceAccessToken(qs:_*)
    post        <- ws.url(s"${WechatClient.gateway }$uri")
    .withQueryString(queryString: _*)
    .withHeaders(HeaderNames.CONTENT_TYPE -> ContentTypes.JSON, HeaderNames.ACCEPT_CHARSET -> "UTF-8")
    .post(Json.toJson(data))
  } yield post

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
          response <- get("cgi-bin/token")("grant_type" -> "client_credential","appid" -> WechatClient.appId,"secret" -> WechatClient.secret)
          web      <-
          {
            val body = response.json
            log.info(s"微信AccessToken响应 =====> $body")
            (body \ "access_token").as[String]
          }.liftF
          //存储一个小时
          _        <- redis.set(redisAccessToken, web, Some(3600L))
        } yield web
      }
    }
  } yield token
  //微信JsApiTicket
  def jsApiTicket: Future[String] = for
  {
    //缓存中JsApiTicket
    cache       <- redis.get[String](redisJsApiTicket)
    //有效JsApiTicket
    jsApiTicket <- cache match
    {
      //缓存获取
      case Some(d) => d.liftF
      //缓存失效 网络请求获取
      case _       => for
      {
        response <- get("cgi-bin/ticket/getticket")("access_token" -> "#", "type" -> "jsapi")
        web      <-
        {
          val body = response.json
          log.info(s"微信JsApiTicket响应 =====> $body")
          (body \ "ticket").as[String]
        }.liftF
        //存储一个小时
        _        <- redis.set(redisJsApiTicket, web, Some(3600L))
      } yield web
    }
  } yield jsApiTicket
  //微信ApiTicket
  def apiTicket: Future[String] = for
  {
    //缓存中ApiTicket
    cache     <- redis.get[String](redisApiTicket)
    //有效ApiTicket
    apiTicket <- cache match
    {
      //缓存获取
      case Some(d) => d.liftF
      //缓存失效 网络请求获取
      case _       => for
      {
        response <- get("cgi-bin/ticket/getticket")("access_token" -> "#", "type" -> "wx_card")
        web      <-
        {
          val body = response.json
          log.info(s"微信ApiTicket响应 =====> $body")
          (body \ "ticket").as[String]
        }.liftF
        //存储一个小时
        _        <- redis.set(redisApiTicket, web, Some(3600L))
      } yield web
    }
  } yield apiTicket

  import com.oasis.osiris.tool.EncryptionTool
  import com.oasis.osiris.wechat.impl.client.WechatClient.{AddTagFans, NewsItem}

  import scala.concurrent.duration._

  /**
    * 运行微信公众号客户端
    * 5秒延迟后开始运行
    * 每一个小时刷新一次
    */
  def startWechat = actorSystem.scheduler.schedule(1.second, 1.hour)
  {
    accessToken.onSuccess
    {
      case _ =>
      if (WechatClient.enableJsApiTicket) jsApiTicket
      if (WechatClient.enableApiTicket) apiTicket
    }
  }

  /**
    * 获取Oauth2
    */
  def oauth2(code: String) = get("sns/oauth2/access_token")(s"appid" -> WechatClient.appId, "secret" -> WechatClient.secret, "code" -> code,
    "grant_type" -> "authorization_code")
  .map(_.json)

  /**
    * js-sdk签名
    */
  def jsSign(timeStamp: String)(nonceStr: String)(uri: String) = for
  {
    ticket <- jsApiTicket
    str    <- s"jsapi_ticket=$ticket&noncestr=$nonceStr&timestamp=$timeStamp&url=$uri".liftF
    sign   <- EncryptionTool.SHA1(str).liftF
  } yield sign

  /**
    * 校验微信服务器签名
    */
  def check(signature: String)(timeStamp: String)(nonce: String) = for
  {
    str    <- Array(WechatClient.token, timeStamp, nonce)
    .sorted
    .reduce(_ + _).liftF
    sign   <- EncryptionTool.SHA1(str).liftF
    result <- sign.equalsIgnoreCase(signature).liftF
  } yield result

  /**
    * 获取指定素材
    */
  def getNewsMaterials(mediaId: String) = for
  {
    request  <- s"""{"media_id":$mediaId}""".liftF
    response <- post("cgi-bin/material/get_material")(request)("access_token" -> "#")
    json     <- Json.toJson(response.body).liftF
    xs       <- (json \\ "news_item").map(_.as[NewsItem]).liftF
  } yield xs

  /**
    * 创建标签
    */
  def createTag(name:String) = for
  {
    request  <- s"""{"name":$name}""".liftF
    response <- post("cgi-bin/tags/create?")(request)("access_token" -> "#")
    wxId     <- (Json.toJson(response.body) \ "tag" \ "id").as[String].liftF
  } yield wxId

  /**
    * 标签添加粉丝
    */
  def addTagFans(request:AddTagFans) = for
  {
    response <- post("cgi-bin/tags/members/batchtagging")(request)("access_token" -> "#")
    json     <- Json.toJson(response.body).liftF
  } yield json

}

object WechatClient
{
  import com.typesafe.config.ConfigFactory
  import play.api.libs.json.{Json, Reads, Writes}
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

  //微信添加粉丝请求
  case class AddTagFans(openIds:Vector[String],wxId:String)

  object AddTagFans
  {
    implicit val writes:Writes[AddTagFans] = Writes[AddTagFans](d => Json.obj("openid_list" -> d.openIds,"tagid" -> d.wxId))
  }

  /**
    * 图文消息素材
    */
  case class NewsItem(title:String,author:String,digest:String,
    content:String,contentSourceUri:String,thumbMediaId:String,
    thumbUri:String,uri:String)

  object NewsItem
  {
    implicit val reads:Reads[NewsItem] = Reads[NewsItem]
    {
      import play.api.libs.json.JsValue
      _.validate[JsValue].map(d => apply((d \ "title").as[String],(d \ "author").as[String],(d \ "digest").as[String],(d \ "content").as[String],
        (d \ "content_source_url").as[String],(d \ "thumb_media_id").as[String],(d \ "thumb_url").as[String],(d \ "url").as[String]))
    }
  }

}
