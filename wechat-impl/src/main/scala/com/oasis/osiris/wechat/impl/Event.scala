package com.oasis.osiris.wechat.impl
import com.lightbend.lagom.scaladsl.persistence.AggregateEvent

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
  import play.api.libs.json.{Format, Json}
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