package com.oasis.osiris.wechat.impl
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType

import play.api.libs.json.{Format, Json}
/**
  * 领域命令
  */
//标签命令集
sealed trait TagCommand[Reply] extends ReplyType[Reply]

object TagCommand
{
  import java.time.Instant

  import akka.Done
  //创建命令
  case class Create(id: String,wxId: String,name: String,createTime: Instant = Instant.now,updateTime: Instant = Instant.now) extends TagCommand[String]

  object Create
  {
    implicit val format:Format[Create] = Json.format
  }

  //添加粉丝命令
  case class AddFans(id: String,openIds: Seq[String] = Nil,updateTime: Instant = Instant.now) extends TagCommand[Done]

  object AddFans
  {
    implicit val format:Format[AddFans] = Json.format
  }
}

//二维码命令集
sealed trait QRCodeCommand[Reply] extends ReplyType[Reply]

object QRCodeCommand
{
  import java.time.Instant

  import com.oasis.osiris.wechat.impl.QRCodeType.QRCodeType

  //创建命令
  case class Create
  (
    id           : String,
    `type`       : QRCodeType,
    sceneStr     : Option[String],
    sceneId      : Option[Int],
    expireSeconds: Option[Int],
    ticket       : String,
    uri          : String,
    createTime   : Instant = Instant.now,
    updateTime   : Instant = Instant.now
  ) extends QRCodeCommand[String]

  object Create
  {
    implicit val format:Format[Create] = Json.format
  }
}
