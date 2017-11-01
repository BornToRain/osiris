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
  import scala.util.Random

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
      redis <- RedisClient(actorSystem).client.liftF
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
        param.toSeq.sorted.foreach
        { case (k, v) => log.info(s" $k => $v") }
        log.info("+---------------------------------------------------------------------------------------------------------------------------+")
        //Map参数构建更新命令
        CallUpRecordCommand.Update.fromMap(param)
      }.liftF
      redis <- RedisClient(actorSystem).client.liftF
      //电话绑定关系Id
      id <- redis.get[BindingRelation](s"$REDIS_KEY_BINDING${cmd.call}")
      .map
      {
        case Some(d) => d.callUpRecordId
        case _ => throw NotFound(s"手机号${cmd.call}")
      }
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
      //阿里短信唯一标识
      messageId <- d.smsType.toApi match
      {
        //达人通知没有验证码
        case SmsType.notice => smsClient.sendNotice(d.mobile)
        //其他都需要4位验证码
        case t =>
        val code = createCode(4)
        t match
        {
          //支付验证码
          case SmsType.payment => smsClient.sendPayment(d.mobile)(code)
          //剩下的目前都发送身份验证验证码
          case _ => smsClient.sendAuthentication(d.mobile)(code)
        }
      }
      //创建命令
      cmd <- SmsRecordCommand.Create(id, d.mobile, d.smsType.toApi, isSuccess = false, messageId).liftF
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

  //产生指定位数纯数字随机码
  private[this] def createCode(i: Int) = Stream.iterate(Random.nextInt(10) + "", i)(_ => Random.nextInt(10) + "").reduce(_ + _)

  /**
    * 主键获取聚合根
    */
  private[this] def refFor[T <: PersistentEntity : ClassTag](id: String) = registry.refFor[T](id)
}
