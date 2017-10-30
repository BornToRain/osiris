package com.oasis.osiris.common.impl

import akka.actor.ActorSystem
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.oasis.osiris.common.api.CommonService
import com.oasis.osiris.common.impl.client.{MoorClient, SmsClient}
import com.oasis.osiris.tool.Api
import com.oasis.osiris.tool.functional.Lift.ops._
import scala.concurrent.ExecutionContext

/**
  * 公用接口实现
  */
class CommonServiceImpl
(
  registry: PersistentEntityRegistry,
  smsClient                : SmsClient,
  moorClient               : MoorClient,
  actorSystem              : ActorSystem,
  callUpRecordRepository: CallUpRecordRepository
)(implicit ec: ExecutionContext) extends CommonService with Api
{
  import com.lightbend.lagom.scaladsl.api.transport.NotFound
  import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
  import com.lightbend.lagom.scaladsl.server.ServerServiceCall
  import com.oasis.osiris.common.impl.client.RedisClient
  import com.oasis.osiris.tool.{IdWorker, Restful}
  import scala.reflect.ClassTag

  //手机号绑定关系在Redis的Key
  private[this] val REDIS_KEY_BINDING = "binding=>"

  override def bindCallUpRecord = v2(ServerServiceCall
  {
    (r, d) =>
    for
    {
      //Id生成
      id <- IdWorker.liftF
      //绑定命令
      cmd <- CallUpRecordCommand.Bind(id, d.thirdId, d.call, d.called, d.maxCallTime, d.noticeUri).liftF
      //发送绑定命令
      _ <- refFor[CallUpRecordEntity](id).ask(cmd)
      //Http201响应
    } yield Restful.created(r)(id)
  })

  override def hangUpCallUpRecord(id: String) = v2(ServerServiceCall
  {
    (_, _) =>
    for
    {
      //发出挂断命令返回挂断请求参数
      hangup <- refFor[CallUpRecordEntity](id).ask(CallUpRecordCommand.HangUp)
      _ <- hangup match
      {
          //调容联七陌接口挂断
        case Some(d) => moorClient.hangUp(d)
        case _       => throw NotFound(s"ID${id }通话记录不存在")
      }
      //Http204响应
    } yield Restful.noContent
  })

  override def calledCallUpRecord(mobile: String) = v2(ServerServiceCall
  {
    _ =>
    for
    {
      redis <- RedisClient(actorSystem).client
      //被呼号
      called <- redis.get[BindingRelation](s"$REDIS_KEY_BINDING$mobile")
      .map
      {
        case Some(d) => d.called
        //Http404响应
        case _ => throw NotFound(s"手机号${mobile }绑定关系不存在")
      }
    } yield called
  })

  override def callbackCallUpRecord = v2(ServerServiceCall
  {
    (r, _) =>
    for
    {
      //参数
      param <- gerParam(r.uri.getQuery).liftF
      //更新命令
      cmd <-
      {
        log.info("通话事件推送更新回调")
        log.info("+---------------------------------------------------------------------------------------------------------------------------+")
        param.foreach
        { case (k, v) => log.info(s" $k => $v") }
        log.info("+---------------------------------------------------------------------------------------------------------------------------+")
        //Map参数构建更新命令
        CallUpRecordCommand.Update.fromMap(param)
      }.liftF
      redis <- RedisClient(actorSystem).client
      //电话绑定关系Id
      id <- redis.get[BindingRelation](s"$REDIS_KEY_BINDING${cmd.call}")
      .map
      {
        case Some(d) => d.callUpRecordId
        case _ => throw NotFound(s"手机号${cmd.call}")
      }
//      newCmd <- CallUpRecordCommand.Update(Some(id),)
      //发送更新命令
      _ <- refFor[CallUpRecordEntity](id).ask(cmd)
      //Http200响应
    } yield Restful.ok
  })

  override def sms = v2(ServerServiceCall
  {
    (r, d) =>
    for
      {
      //主键
      id <- IdWorker.liftF
//      code <- d.smsType match
//        {
//
//      }
      //创建命令
      cmd <- SmsRecordCommand.Create(id, d.mobile, d.smsType, isSuccess = false).liftF
      //发送创建命令
      _ <- refFor[SmsRecordEntity](id).ask(cmd)
      //Http201响应
    } yield Restful.created(r)(id)
  })

  override def smsValidation = ???

  /**
    * 从QueryString获取Map集合参数
    */
  private[this] def gerParam(query: String) = query
  .split("&")
  .map(_.split("="))
  .foldLeft(Map.empty[String, String])
  {
    case (map, Array(x, y)) => map + (x -> y)
    case (map, _)           => map
  }

  /**
    * 主键获取聚合根
    */
  private[this] def refFor[T <: PersistentEntity : ClassTag](id: String) = registry.refFor[T](id)
}
