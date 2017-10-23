package com.oasis.osiris.common.impl

import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag}
import com.oasis.osiris.common.impl.CallUpRecordCommand.Bind
import play.api.libs.json.{Format, Json}
import com.oasis.osiris.tool.JSONTool._

/**
	* 领域事件
	*/
//通话记录事件集
sealed trait CallUpRecordEvent extends AggregateEvent[CallUpRecordEvent]
{
	def aggregateTag = CallUpRecordEvent.tag
}

object CallUpRecordEvent
{
	//按事件数分片
	val numberShared = 2
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
		implicit val format:Format[HungUp.type ] = singletonFormat(HungUp)
	}
}