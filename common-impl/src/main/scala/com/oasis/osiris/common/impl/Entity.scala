package com.oasis.osiris.common.impl

import akka.actor.ActorSystem
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity

/**
  * 持久化
  */
//通话记录持久化
class CallUpRecordEntity(actorSystem: ActorSystem) extends PersistentEntity
{
  import java.time.Duration
  import com.oasis.osiris.common.impl.CallUpRecordCommand._
  import com.oasis.osiris.common.impl.CallUpRecordEvent._
  import com.oasis.osiris.common.impl.client.{MoorRequest, RedisClient}

  override type Command = CallUpRecordCommand[_]
  override type Event = CallUpRecordEvent
  override type State = Option[CallUpRecord]

  override def initialState = None

  override def behavior =
  {
    //不存在
    case None => nonexistence
    //存在状态
    case _ => existence
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
    val redis = RedisClient(actorSystem).client
    //绑定关系存入Redis 30分钟
    redis.set[BindingRelation](s"$REDIS_KEY_BINDING${cmd.call }", BindingRelation(cmd.id, cmd.call, cmd.called, cmd.thirdId),
      Some(Duration.ofMinutes(30L).getSeconds))
    Some(CallUpRecord.bind(cmd))
  }

  //存在状态下操作
  def existence = Actions()
  //处理挂断命令
  .onCommand[HangUp.type, Option[MoorRequest.HangUp]]
  {
    //持久化挂断事件回复挂断请求参数
    case (_, ctx, state) => ctx.thenPersist(HungUp)
    {
      _ => state.map(d => MoorRequest.HangUp(d.callId, None, d.id))
    }
  }
  //处理更新命令
  .onCommand[Update, Option[CallUpRecord]]
  {
    //持久化更新事件回复聚合根
    case ((cmd: Update), ctx, state) => ctx.thenPersist(Updated(cmd))(_ => ctx.reply(state))
  }
  .onEvent
  {
    case (_: HungUp.type, state) => state.map
    {
      d =>
        //删除Redis绑定关系
      val redis = RedisClient(actorSystem).client
      redis.del(s"$REDIS_KEY_BINDING${d.call }")
      d
    }
    //更新聚合根
    case (event: Updated, state) => state.map(_.update(event))
  }
}

//短信记录持久化
class SmsRecordEntity(actorSystem: ActorSystem) extends PersistentEntity
{
  import com.oasis.osiris.common.impl.SmsRecordCommand._
  import com.oasis.osiris.common.impl.SmsRecordEvent._
  import com.oasis.osiris.common.impl.client.RedisClient

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

  //不存在状态下操作
  def nonexistence = Actions()
  //处理创建命令
  .onCommand[Create, String]
  {
    //持久化创建事件回复聚合根Id
    case ((cmd: Create), ctx, _) => ctx.thenPersist(Created(cmd))
    {
      e =>
      cmd.code.map
      {
        val redis = RedisClient(actorSystem).client
        //Redis记录用户与验证码关系 5分钟验证码过期
        redis.set(s"${cmd.smsType }=>${cmd.mobile }", _, Some(300L))
      }
      ctx.reply(e.cmd.id)
    }
  }
  .onEvent
  {
    //创建聚合根
    case (Created(cmd), _) => Some(SmsRecord(cmd.id, cmd.messageId, cmd.mobile, cmd.smsType, cmd.isSuccess, cmd.createTime, cmd.updateTime))
  }
}
