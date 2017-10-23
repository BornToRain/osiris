package com.oasis.osiris.common.impl

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity

/**
	* 通话记录持久化
	*/
class CallUpRecordEntity extends PersistentEntity
{

	import com.oasis.osiris.common.impl.CallUpRecordCommand._
	import com.oasis.osiris.common.impl.CallUpRecordEvent._

	override type Command = CallUpRecordCommand[_]
	override type Event = CallUpRecordEvent
	override type State = CallUpRecordState
	override def initialState = CallUpRecordState.nonexistence
	override def behavior =
	{
		//不存在
		case CallUpRecordState.nonexistence => nonexistence
		//存在状态
		case CallUpRecordState(Some(_), _, _) => existence
	}

	//不存在状态下操作
	def nonexistence = Actions()
	//处理绑定命令
	.onCommand[Bind, Done]
	{
		//持久化绑定事件回复完成
		case (cmd: Bind, ctx, _) => ctx.thenPersist(Bound(cmd))(_ => ctx.reply(Done))
	}
	//处理事件
	.onEvent
	{
		//创建聚合根
		case (Bound(cmd), _) =>
		val data = CallUpRecord(cmd.id, cmd.call, cmd.called, cmd.maxCallTime, None, None, None, None, None, None, cmd.noticeUri, cmd.thirdId, None,
			cmd.createTime, cmd.updateTime)
		CallUpRecordState(Some(data), None, None)
	}

	//存在状态下操作
	def existence = Actions()
	//处理挂断命令
	.onCommand[HangUp.type, Done]
	{
		//持久化挂断事件回复完成
		case (_, ctx, _) => ctx.thenPersist(HungUp)(_ => ctx.reply(Done))
	}
	.onEvent
	{
		//不处理事件
		case (_:HungUp.type ,state) => state
	}
}
