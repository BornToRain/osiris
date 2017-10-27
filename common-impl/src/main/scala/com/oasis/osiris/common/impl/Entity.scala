package com.oasis.osiris.common.impl

import com.lightbend.lagom.scaladsl.persistence.PersistentEntity

/**
  * 持久化
  */
//通话记录持久化
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
  .onCommand[Bind, String]
  {
    //持久化绑定事件回复聚合根Id
    case (cmd: Bind, ctx, _) => ctx.thenPersist(Bound(cmd))(_ => ctx.reply(cmd.id))
  }
  //处理事件
  .onEvent
  {
    //创建聚合根
    case (Bound(cmd), _) =>
    val data = CallUpRecord.bind(cmd)
    CallUpRecordState(Some(data), None, None)
  }

  //存在状态下操作
  def existence = Actions()
  //处理挂断命令
  .onCommand[HangUp.type, Option[MoorRequest.HangUp]]
  {
    //持久化挂断事件回复挂断请求参数
    case (_, ctx, state) => ctx.thenPersist(HungUp)
    {
      _ => state.data.map(d => MoorRequest.HangUp(d.callId, None, d.id))
    }
  }
  //处理更新命令
  .onCommand[Update, Option[CallUpRecord]]
  {
    //持久化更新事件回复聚合根
    case ((cmd: Update), ctx, state) => ctx.thenPersist(Updated(cmd))(_ => ctx.reply(state.data))
  }
  .onEvent
  {
    //不处理挂断事件
    case (_: HungUp.type, state) => state
    //更新聚合根
    case (event: Updated, state) => state.update(event)
  }
}

class SmsRecordEntity extends PersistentEntity
{
  import com.oasis.osiris.common.impl.SmsRecordCommand._
  import com.oasis.osiris.common.impl.SmsRecordEvent._

  override type Command = SmsRecordCommand[_]
  override type Event = SmsRecordEvent
  override type State = Option[SmsRecord]

  override def initialState = None

  override def behavior =
  {
    //不存在
    case None => nonexistence
    case _    => nonexistence
  }

  def nonexistence = Actions()
  //处理创建命令
  .onCommand[Create, String]
  {
    //持久化创建事件回复聚合根Id
    case ((cmd: Create), ctx, _) => ctx.thenPersist(Created(cmd))(e => ctx.reply(e.cmd.id))
  }
  .onEvent
  {
    //创建聚合根
    case (event: Created, state) => state
  }
}
