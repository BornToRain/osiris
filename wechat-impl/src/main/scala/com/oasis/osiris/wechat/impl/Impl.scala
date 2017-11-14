package com.oasis.osiris.wechat.impl
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.oasis.osiris.tool.Api
import com.oasis.osiris.wechat.api.WechatService
import com.oasis.osiris.wechat.impl.client.WechatClient
import redis.RedisClient

import scala.concurrent.ExecutionContext

/**
  * 微信接口实现
  */
class WechatServiceImpl
(
  redis         : RedisClient,
  wechat        : WechatClient,
  registry      : PersistentEntityRegistry,
  menuRepository: MenuRepository
)(implicit ec: ExecutionContext) extends WechatService with Api
{
  import com.lightbend.lagom.scaladsl.api.transport.BadRequest
  import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
  import com.lightbend.lagom.scaladsl.server.ServerServiceCall
  import com.oasis.osiris.tool.{IdWorker, Restful}
  import com.oasis.osiris.tool.functional.Lift.ops._
  import com.oasis.osiris.wechat.api
  import com.oasis.osiris.wechat.api.WechatRequest
  import com.oasis.osiris.wechat.impl.tool.XMLTool

  import scala.reflect.ClassTag
  import scala.util.Random

  override def createTag = v2(ServerServiceCall
  {
    (r,d) =>
    for
    {
      //主键生成
      id   <- IdWorker.liftF
      //请求微信创建标签
      wxId <- wechat.createTag(d.name)
      //创建命令
      cmd  <- TagCommand.Create(id,wxId,d.name).liftF
      //发送创建命令
      _    <- refFor[TagEntity](id).ask(cmd)
      //Http201响应
    } yield Restful.created(r)(id)
  })

  override def createQRCode = v2(ServerServiceCall
  {
    (r,d) =>

    import com.oasis.osiris.wechat.impl.QRCodeType.QRCodeType
    import com.oasis.osiris.wechat.impl.client.WechatClient.CreateQRCode

    for
    {
      //主键生成
      id         <- IdWorker.liftF
      actionName <- d.`type`.toDomain[QRCodeType].liftF
      //构建请求微信二维码参数
      request    <- d match
      {
        case api.QRCodeDTO.Create(_, None, Some(i), second) => CreateQRCode(actionName,("scene_id",i.toString),second).liftF
        case api.QRCodeDTO.Create(_, Some(s), None, second) => CreateQRCode(actionName,("scene_str",s),second).liftF
      }
      //请求微信创建二维码
      ticket     <- wechat.createQRCode(request)
      //请求微信创建短链接
      uri        <- wechat.createShortURI(ticket)
      //创建命令
      cmd        <- QRCodeCommand.Create(id,actionName,d.sceneStr,d.sceneId,d.expireSeconds,ticket,uri).liftF
      //发送创建命令
      _          <- refFor[QRCodeEntity](id).ask(cmd)
      //Http201响应
    } yield Restful.created(r)(id)
  })

  override def getOpenId(code: String) = v2(ServerServiceCall
  {
    _ =>
    import play.api.libs.json.Json
    for
    {
      json   <- wechat.oauth2(code)
      openId <- (json \ "openid").asOpt[String].liftF
      result <- openId match
      {
        case Some(d) => Json.obj("openid" -> d).liftF
        case _       => throw BadRequest("无效的code值")
      }
    } yield result
  })

  override def getJsSDK(uri: String) = v2(ServerServiceCall
  {
    _ =>
    import com.oasis.osiris.tool.DateTool
    for
    {
      timestamp <- DateTool.datetimeStamp.liftF
      nonceStr  <- createRandom(32).liftF
      signature <- wechat.jsSign(timestamp)(nonceStr)(uri)
      result    <- api.JsSDK(WechatClient.appId,timestamp,nonceStr,signature).liftF
    } yield result
  })

  //产生指定位数随机数
  private[this] def createRandom(i: Int) = Stream.iterate(Random.nextPrintableChar(), i)(_ => Random.nextPrintableChar()).mkString

  override def resetMenu = v2(ServerServiceCall
  {
    _ =>
    for
    {
      //菜单数据
      request <- menuRepository.getReset
      //请求微信重置菜单
      result <- wechat.resetMenu(request)
    } yield result
 })

  override def createMenu = v2(ServerServiceCall
  {
    (r,d) =>
    import com.oasis.osiris.wechat.impl.MenuType.MenuType

    for
    {
      //主键生成
      id <- IdWorker.liftF
      t <- d.`type`.toDomain[MenuType].liftF
      //创建命令
      cmd <- MenuCommand.Create(id,t,d.name,d.key,d.uri,d.parentId,d.sort,d.isShow).liftF
      //发送创建命令
      _ <- refFor[MenuEntity](id).ask(cmd)

    } yield Restful.created(r)(id)
  })

  override def get(signature: String, timestamp: String, nonce: String, echostr: String) = v2(ServerServiceCall
  {
    _ =>
    log.info("校验消息来源是否微信")
    wechat.check(signature)(timestamp)(nonce).map
    {
      case true => echostr
      case _    => ""
    }
  })

  override def post = logged(ServerServiceCall
  {
    d =>
    import com.oasis.osiris.wechat.api.WechatRequest

    val request = WechatRequest.parseXML(d)
    val openId  = request.FromUserName
    val msgType = request.MsgType

    log.info("处理微信消息(XML)")
    log.info(s"唯一标识 =====> $openId")
    log.info(s"消息类型 =====> $msgType")

    for
    {
      result <- msgType match
      {
        //事件处理
        case "event" => eventHandle(request)
        //临时统一处理
        case _       => textResponse(request)("泓华医疗,让健康更简单!").liftF
      }
      //Http200响应
    } yield result
  })

  /**
    * 事件处理
    */
  private[this] def eventHandle(request: WechatRequest) =
  {
    val event = request.Event

    log.info(s"事件     =====> $event")
    log.info(s"事件Key  =====> ${request.EventKey}")

    for
    {
      //事件
      result <- event.get match
      {
        case "LOCATION"                 => locationEvent(request)
        case e @ ("SCAN" | "subscribe") => subscribeEvent(request)(e)
        case _                          => success.liftF
      }
    } yield result
  }

  /**
    * 上报地理位置事件
    */
  private[this] def locationEvent(request:WechatRequest) =
  {
    log.info("用户地理位置")

    val openId    = request.FromUserName
    //纬度
    val latitude  = request.Latitude
    //经度
    val longitude = request.Longitude
    //精度
    val precision = request.Precision

    //修改用户地理位置
    for
    {
      //用户微信信息
      data   <- redis.get[OpenId](openId)
      //更新地理位置
      update <- data.map
      {
        d =>
        val location = d.location.map(_.copy(latitude = latitude, longitude = longitude, precision = precision))
        d.copy(location = location)
      }.getOrElse(OpenId(None, Some(Location(latitude, longitude, None, None, precision)), isFollow = false))
      .liftF
      _      <- redis.set[OpenId](openId, update)
    } yield success
  }

  /**
    * 关注、已关注事件
    */
  private[this] def subscribeEvent(request:WechatRequest)(event:String) =
  {
    log.info("用户关注")

    for
    {
      f      <- (newsHandle(request)_).liftF
      result <- request.EventKey match
      {
        //有绑定图文
        case Some(d) if d.contains("clinic") =>
        val openId = request.FromUserName
        /**
          * 诊所ID
          * 已关注格式 clinicId=1
          * 首次关注格式 qrscene_clinicId=1
          */
        val id = if (event == "SCAN") d.split("=")(1)
        else d.substring(8).split("=")(1)

        for
        {
          //用户微信信息
          data   <- redis.get[OpenId](openId)
          //更新绑定诊所
          update <- data.map(_.copy(clinic = Some(id),isFollow = true)).getOrElse(OpenId(Some(id), None, isFollow = true)).liftF
          _      <- redis.set[OpenId](openId,update)
        } yield update
        f(true)
        //无绑定图文
        case _                               => f(false)
      }
    } yield result
  }

  /**
    * 处理图文消息
    */
  private[this] def newsHandle(request: WechatRequest)(bind: Boolean) = for
  {
    //获取素材
    xs       <- wechat.getNewsMaterials("6Xjge0ynQGVPMd5ib0xckh76O4Unu0uYXJPv5OCVqfE")
    //素材转换微信需要的图文消息格式
    articles <- xs.map
    {
      //有诊所返回诊所详情
      d =>
      if (bind) WechatResponse.NewsArticle(d.title, d.digest, d.thumbUri, "https://m.oasiscare.cn/wxofficial/clinicdetails.html")
      else WechatResponse.NewsArticle(d.title, d.digest, d.thumbUri, d.uri)
    }.liftF
    //公众号图文响应
    result   <- newsResponse(request)(articles).liftF
  } yield result

  /**
    * 文本响应
    */
  private[this] def textResponse(request:WechatRequest)(content:String) = zip(WechatResponse.Text(request.FromUserName,request.ToUserName,request
  .CreateTime,"text",content))

  /**
    * 图文响应
    */
  private[this] def newsResponse(request:WechatRequest)(articles:Seq[WechatResponse.NewsArticle]) = zip(WechatResponse.News(request.FromUserName,
    request.ToUserName,request.CreateTime,"news",articles.length,articles))

  /**
    * 封装响应成XML
    */
  private[this] def zip(response:WechatResponse) =
  {
    val xml = XMLTool.toXML(response)
    log.info(s"响应微信 =====> $xml")
    xml
  }

  /**
    * 主键获取聚合根
    */
  private[this] def refFor[T <: PersistentEntity : ClassTag](id: String) = registry.refFor[T](id)
}

