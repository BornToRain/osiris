package com.oasis.osiris.wechat.impl

import akka.util.ByteString
import play.api.libs.json.{Format, Json}
import redis.ByteStringFormatter

/**
  * 领域值对象
  */
case class OpenId
(
  clinic  : Option[String],
  location: Option[Location],
  isFollow: Boolean
)

object OpenId
{
  implicit val format: Format[OpenId] = Json.format
  implicit val redisFormat            = new ByteStringFormatter[OpenId]
  {
    override def serialize(data: OpenId) = ByteString(s"${Json.toJson(data) }")

    override def deserialize(bs: ByteString) = Json.parse(bs.utf8String).as[OpenId]
  }
}

//用户地理位置
case class Location
(
  latitude : Option[String],
  longitude: Option[String],
  scale    : Option[String],
  label    : Option[String],
  precision: Option[String]
)

object Location
{
  implicit val format: Format[Location] = Json.format
  implicit val redisFormat              = new ByteStringFormatter[Location]
  {
    override def serialize(data: Location) = ByteString(s"${Json.toJson(data) }")

    override def deserialize(bs: ByteString) = Json.parse(bs.utf8String).as[Location]
  }
}

sealed trait WechatResponse

object WechatResponse
{

  //文本消息响应
  case class Text
  (
    //接收方帐号收到的OpenID）
    ToUserName  : String,
    //开发者微信号
    FromUserName: String,
    //消息创建时间整型）
    CreateTime  : String,
    //消息类型
    MsgType     : String,
    //文本
    Content     : String
  ) extends WechatResponse

  //图文消息图文
  case class NewsArticle
  (
    //图文消息名称
    Title      : String,
    //图文消息描述
    Description: String,
    //图片链接，支持JPG、PNG格式，较好的效果为大图640*320，小图80*80，限制图片链接的域名需要与开发者填写的基本资料中的Url一致
    PicUrl     :String,
    //点击图文消息跳转链接
    Url        :String
  ) extends WechatResponse

  //图文消息
  case class News
  (
    //接收方账号(收到的openId)
    ToUserName  : String,
    //开发者微信号
    FromUserName: String,
    //消息创建时间(整型)
    CreateTime  : String,
    //消息类型
    MsgType     : String,
    //图文消息个数,限制为8条以内
    ArticleCount: Int,
    //多条图文消息信息,默认第一个item为大图
    Articles    : Seq[NewsArticle]
  ) extends WechatResponse


}


