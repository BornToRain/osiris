package com.oasis.osiris.wechat.impl
import com.lightbend.lagom.scaladsl.persistence.AggregateEvent

import play.api.libs.json.{Format, Json}
/**
  * 领域事件
  */
//标签事件集
sealed trait TagEvent extends AggregateEvent[TagEvent]
{
  import com.lightbend.lagom.scaladsl.persistence.AggregateEventTagger

  override def aggregateTag: AggregateEventTagger[TagEvent] = TagEvent.tag
}

object TagEvent
{
  import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
  import com.oasis.osiris.wechat.impl.TagCommand.{AddFans, Create}
  //按事件数分片
  val numberShared = 2
  val tag          = AggregateEventTag.sharded[TagEvent](numberShared)

  //创建事件
  case class Created(cmd:Create) extends TagEvent

  object Created
  {
    implicit val format:Format[Created] = Json.format
  }

  //添加粉丝事件
  case class AddedFans(cmd:AddFans) extends TagEvent

  object AddedFans
  {
    implicit val format:Format[AddedFans] = Json.format
  }
}

//二维码事件集
sealed trait QRCodeEvent extends AggregateEvent[QRCodeEvent]
{
  import com.lightbend.lagom.scaladsl.persistence.AggregateEventTagger
  override def aggregateTag: AggregateEventTagger[QRCodeEvent] = QRCodeEvent.tag
}

object QRCodeEvent
{
  import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
  import com.oasis.osiris.wechat.impl.QRCodeCommand.Create
  //按事件数分片
  val numberShared = 1
  val tag          = AggregateEventTag.sharded[QRCodeEvent](numberShared)

  //创建事件
  case class Created(cmd:Create) extends QRCodeEvent

  object Created
  {
    implicit val format:Format[Created] = Json.format
  }
}