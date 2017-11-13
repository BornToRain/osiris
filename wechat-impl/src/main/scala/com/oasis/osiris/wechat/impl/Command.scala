package com.oasis.osiris.wechat.impl
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType

/**
  * 领域命令
  */
//标签命令集
sealed trait TagCommand[Reply] extends ReplyType[Reply]

object TagCommand
{
  import java.time.Instant
  import play.api.libs.json.{Format, Json}

  import akka.Done
  //创建命令
  case class Create(id:String,wxId:String,name:String,createTime:Instant = Instant.now,updateTime:Instant = Instant.now) extends TagCommand[String]

  object Create
  {
    implicit val format:Format[Create] = Json.format
  }

  //添加粉丝命令
  case class AddFans(id:String,openIds:Vector[String] = Vector.empty,updateTime:Instant = Instant.now) extends TagCommand[Done]

  object AddFans
  {
    implicit val format:Format[AddFans] = Json.format
  }
}
