package com.oasis.osiris.wechat.impl

import java.time.Instant

import com.oasis.osiris.tool.JSONTool._
import com.oasis.osiris.wechat.impl.MenuType.MenuType
import com.oasis.osiris.wechat.impl.QRCodeType.QRCodeType
import play.api.libs.json.{Format, Json}

/**
  * 领域聚合根
  */
//标签聚合根
case class Tag
(
  id        : String,
  //微信标签ID
  wxId      : String,
  name      : String,
  //粉丝数
  fans      : Long,
  //粉丝列表
  openIds   : Vector[String],
  createTime: Instant,
  updateTime: Instant
)
{
  /**
    * 添加粉丝
    */
  def addFans(openId: String*):Tag = copy(openIds = openIds ++ openId)
}

object Tag
{
  implicit val format:Format[Tag] = Json.format
}

//二维码聚合根
case class QRCode
(
  id           : String,
  //二维码类型，QR_SCENE为临时,QR_LIMIT_SCENE为永久,QR_LIMIT_STR_SCENE为永久的字符串参数值
  `type`       : QRCodeType,
  //场景值ID（字符串形式的ID），字符串类型，长度限制为1到64，仅永久二维码支持此字段
  sceneStr     : Option[String],
  //场景值ID，临时二维码时为32位非0整型，永久二维码时最大值为100000（目前参数只支持1--100000）
  sceneId      : Option[Int],
  //该二维码有效时间，以秒为单位。 最大不超过2592000（即30天），此字段如果不填，则默认有效期为30秒。
  expireSeconds: Option[Int],
  //获取的二维码ticket，凭借此ticket可以在有效时间内换取二维码。
  ticket       : String,
  //转成短链接二维码
  uri          : String,
  createTime   : Instant,
  updateTime   : Instant
)

object QRCode
{
  implicit val format:Format[QRCode] = Json.format
}

//二维码类型
object QRCodeType extends Enumeration
{
  type QRCodeType = Value
  //临时的整型参数、临时的字符串参数、永久的整型参数、永久的字符串参数
  val QR_SCENE,QR_STR_SCENE,QR_LIMIT_SCENE,QR_LIMIT_STR_SCENE = Value

  implicit val format:Format[QRCodeType] = enumFormat(QRCodeType)
}

//菜单聚合根
case class Menu
(
  id        : String,
  //菜单名称
  name      : String,
  //菜单类型
  `type`    : Option[MenuType],
  //菜单键值 Key类型必传
  key       : Option[String],
  //菜单uri view类型必传
  uri       : Option[String],
  //父菜单Id
  parentId  : Option[String],
  //排序
  sort      : Int,
  //是否展示
  isShow    : Boolean,
  createTime: Instant = Instant.now,
  updateTime: Instant = Instant.now
)

object Menu
{
  implicit val format:Format[Menu] = Json.format
}

//菜单类型
object MenuType extends Enumeration
{
  type MenuType = Value
  val click,view = Value

  implicit val format:Format[MenuType] = enumFormat(MenuType)
}