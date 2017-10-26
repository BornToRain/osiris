package com.oasis.osiris.common.impl

import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger}
import com.oasis.osiris.tool.JSONTool._
import play.api.libs.json.{Format, Json}

/**
	* 领域事件
	*/
//通话记录事件集
sealed trait CallUpRecordEvent extends AggregateEvent[CallUpRecordEvent]
{
	override def aggregateTag: AggregateEventTagger[CallUpRecordEvent] = CallUpRecordEvent.tag
}

object CallUpRecordEvent
{

	import com.oasis.osiris.common.impl.CallUpRecordCommand._

	//按事件数分片
	val numberShared = 3
	val tag          = AggregateEventTag.sharded[CallUpRecordEvent](numberShared)

	//绑定事件
	case class Bound(cmd: Bind) extends CallUpRecordEvent

	object Bound
	{
		implicit val format: Format[Bound] = Json.format
	}

	//挂断事件
	case object HungUp extends CallUpRecordEvent
	{
		implicit val format: Format[HungUp.type] = singletonFormat(HungUp)
	}

	//更新时间
	case class Updated(cmd: Update) extends CallUpRecordEvent

	object Updated extends CallUpRecordEvent
	{
		implicit val format: Format[Updated] = Json.format
	}

}

//短信记录事件集
sealed trait SmsRecordEvent extends AggregateEvent[SmsRecordEvent]
{
	override def aggregateTag: AggregateEventTagger[SmsRecordEvent] = ???
}

object SmsRecordEvent
{

	import com.oasis.osiris.common.impl.SmsRecordCommand._

	//按事件数分片
	val numberShared = 1
	val tag          = AggregateEventTag.sharded[SmsRecordEvent](numberShared)

	//创建事件
	case class Created(cmd: Create) extends SmsRecordEvent

	object Created
	{
		implicit val format: Format[Created] = Json.format
	}

}