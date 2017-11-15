package com.oasis.osiris.wechat.api

import com.oasis.osiris.tool.JSONTool.enumFormat
import play.api.libs.json.{Format, Json, Writes}

/**
  * 接口层DTO对象
  */
object TagDTO
{

  //创建DTO
  case class Create(name: String)

  object Create
  {
    implicit val format: Format[Create] = Json.format
  }

}

object QRCodeDTO
{
  import com.oasis.osiris.wechat.api.QRCodeDTO.QRCodeType.QRCodeType

  //创建DTO
  case class Create(`type`: QRCodeType, sceneStr: Option[String], sceneId: Option[Int], expireSeconds: Option[Int])

  object Create
  {
    implicit val format: Format[Create] = Json.format
  }

  //二维码类型
  object QRCodeType extends Enumeration
  {
    type QRCodeType = Value
    //临时的整型参数、临时的字符串参数、永久的整型参数、永久的字符串参数
    val QR_SCENE, QR_STR_SCENE, QR_LIMIT_SCENE, QR_LIMIT_STR_SCENE = Value
    implicit val format: Format[QRCodeType] = enumFormat(QRCodeType)
  }

}

object MenuDTO
{
  import com.oasis.osiris.wechat.api.MenuDTO.MenuType.MenuType

  //创建DTO
  case class Create(`type`: Option[MenuType], name: String, key: Option[String], uri: Option[String], parentId: Option[String], sort: Int, isShow: Boolean)

  object Create
  {
    implicit val format: Format[Create] = Json.format
  }

  //菜单类型
  object MenuType extends Enumeration
  {
    type MenuType = Value
    val click, view = Value
    implicit val format: Format[MenuType] = enumFormat(MenuType)
  }

}

case class JsSDK(appId: String, timestamp: String, nonceStr: String, signature: String)

object JsSDK
{
  import play.api.libs.json.Reads

  implicit val reads: Reads[JsSDK]  = Json.reads
  implicit val writes: Writes[JsSDK] = Writes(
    d => Json.obj("appId" -> d.appId, "timeStamp" -> d.timestamp, "nonceStr" -> d.nonceStr, "signature" -> d.signature))
}

/**
  * 微信公众号请求参数
  */
case class WechatRequest
(
  //公众号
  ToUserName    : String,
  //用户OpenId
  FromUserName  : String,
  //消息创建时间(整型)
  CreateTime    : String,
  //消息类型
  MsgType       : String,
  //消息id,64位整形
  MsgId         : String,
  /** *****文本消息 *******/
  //文本消息内容
  Content       : Option[String],
  /** *****图片消息 *******/
  //图片链接（由系统生成）
  PicUrl        : Option[String] = None,
  //图片消息媒体id，可以调用多媒体文件下载接口拉取数据。
  MediaId       : Option[String],
  /** *****语音消息 *******/
  //语音格式，如amr，speex等
  Format        : Option[String],
  //语音识别结果，UTF8编码
  Recognition   : Option[String],
  /** *****(小)视频消息 *******/
  //视频消息缩略图的媒体id，可以调用多媒体文件下载接口拉取数据。
  ThumbMediaId  : Option[String],
  /** *****地理位置消息 *******/
  //地理位置纬度
  Location_X    : Option[String],
  //地理位置经度
  Location_Y    : Option[String],
  //地图缩放大小
  Scale         : Option[String],
  //地理位置信息
  Label         : Option[String],
  //消息标题
  Title         : Option[String],
  //消息描述
  Description   : Option[String],
  //消息链接
  Url           : Option[String],
  /** *****接收事件推送 *******/
  //事件类型
  Event: Option[String],
  //事件KEY值
  EventKey      : Option[String],
  //二维码的ticket，可用来换取二维码图片
  Ticket        : Option[String],
  //纬度
  Latitude      : Option[String],
  //经度
  Longitude     : Option[String],
  //精度
  Precision     : Option[String]
)

object WechatRequest
{
  import scala.xml.{Elem, NodeSeq}

  implicit def nodeSeqToValue(node: NodeSeq) = node.text match
  {
    case "" => None
    case d  => Some(d)
  }

  def parseXML(xml: Elem) = apply((xml \ "ToUserName").text, (xml \ "FromUserName").text, (xml \ "CreateTime").text, (xml \ "MsgType").text,
    (xml \ "MsgId").text,
    xml \ "Content", xml \ "PicUrl", xml \ "MediaId", xml \ "Format", xml \ "Recognition", xml \ "ThumbMediaId", xml \ "Location_X",
    xml \ "Location_Y",
    xml \ "Scale", xml \ "Label", xml \ "Title", xml \ "Description", xml \ "Url", xml \ "Event", xml \ "EventKey", xml \ "Ticket", xml \ "Latitude",
    xml \ "Longitude",
    xml \ "Precision")

}
