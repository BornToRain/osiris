package com.oasis.osiris.common.impl
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.oasis.osiris.common.api.CommonService
import com.oasis.osiris.tool.Api
import com.oasis.osiris.tool.functional.Lift.ops._

import scala.concurrent.ExecutionContext

/**
  * 公用接口实现
  */
class CommonServiceImpl
(
  moor                     : MoorClient,
  registry                 : PersistentEntityRegistry,
  callUpRecordRepository   : CallUpRecordRepository,
  bindingRelationRepository: BindingRelationRepository
)(implicit ec: ExecutionContext) extends CommonService with Api
{
  import akka.Done
  import com.lightbend.lagom.scaladsl.api.transport.NotFound
  import com.lightbend.lagom.scaladsl.server.ServerServiceCall
  import com.oasis.osiris.tool.{IdWorker, Restful}

  override def bindCallUpRecord = v2(ServerServiceCall((r, d) =>
  {
    for
    {
      //Id生成
      id <- IdWorker.liftF
      //绑定命令
      cmd <- CallUpRecordCommand.Bind(id, d.thirdId, d.call, d.called, d.maxCallTime, d.noticeUri).liftF
      //发送绑定命令
      _ <- refFor(id).ask(cmd)
      //Http201响应
    } yield Restful.created(r)(id)
  }))
  override def hangUpCallUpRecord(id: String) = v2(ServerServiceCall((_, _) =>
  {
    for
    {
      //发出挂断命令
      _ <- refFor(id).ask(CallUpRecordCommand.HangUp)
      //Http204响应
    } yield Restful.noContent
  }))
  override def calledCallUpRecord(mobile: String) = v2(ServerServiceCall(_ =>
  {
    log.info(s"容联七陌电话回调 =====> $mobile")
    //绑定关系
    bindingRelationRepository.getByPK(mobile)
    .map
    {
      case Some((called, _)) => called
      //Http404响应
      case _ => throw NotFound(s"手机号${mobile }绑定关系不存在")
    }
  }))
  override def callbackCallUpRecord = v2(ServerServiceCall((r, _) =>
  {
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
      //电话绑定关系Id
      id <- bindingRelationRepository.getByPK(cmd.call).map
      {
        case Some((_, id)) => id
        case _             => throw NotFound("绑定关系不存在")
      }
      //发送更新命令
      data <- refFor(id).ask(cmd)
      _ <- data.withFilter(_.noticeUri.isDefined)
      .map(d => d)
      //Http200响应
    } yield Restful.ok
  }))
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
  override def sms = v2(ServerServiceCall(d =>
  {
    for
    {
      //主键
      id <- IdWorker.liftF
      //创建命令
      cmd <- SmsRecordCommand.Create(id, d.mobile, d.smsType, isBusiness = false).liftF
    } yield Done
  }))
  /**
    * 主键获取聚合根
    */
  private[this] def refFor(id: String) = registry.refFor[CallUpRecordEntity](id)
}
