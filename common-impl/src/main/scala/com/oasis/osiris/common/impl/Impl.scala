package com.oasis.osiris.common.impl

import com.lightbend.lagom.scaladsl.api.transport.NotFound
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.oasis.osiris.common.api.CommonService
import com.oasis.osiris.common.impl.CallUpRecordCommand._
import com.oasis.osiris.tool.functional.Lift.ops._
import com.oasis.osiris.tool.{Api, IdWorker, Restful}

import scala.concurrent.ExecutionContext

/**
	* 公用接口实现
	*/
class CommonServiceImpl
(
	registry: PersistentEntityRegistry,
	callUpRecordRepository: CallUpRecordRepository,
	bindingRelationRepository: BindingRelationRepository
)
(implicit ec: ExecutionContext) extends CommonService with Api
{
	override def bindCallUpRecord = v2(ServerServiceCall
	{
		(request, data) =>
		for
		{
			//Id生成器
			id <- IdWorker.liftF
			//绑定命令
			cmd <- Bind(id, data.thirdId, data.call, data.called, data.maxCallTime, data.noticeUri).liftF
			//发送绑定命令
			_ <- refFor(id).ask(cmd)
			//Http201响应
		} yield Restful.created(request)(id)
	})

	override def hangUpCallUpRecord(id: String) = v2(ServerServiceCall
	{
		(_, _) =>
		for
		{
			//发出挂断命令
			_ <- refFor(id).ask(HangUp)
			//Http204响应
		} yield Restful.noContent
	})

	override def calledCallUpRecord(mobile: String) = v2(ServerServiceCall
	{
		_ =>
		log.info(s"容联七陌电话回调 =====> $mobile")
		//绑定关系
		bindingRelationRepository.getByPK(mobile)
		.map
		{
			case Some((called, _)) => called
			//404 NotFound
			case _ => throw NotFound(s"手机号${mobile }绑定关系不存在")
		}
	})

	override def callbackCallUpRecord = v2(ServerServiceCall
	{
		(request, _) =>
		for
		{
			//参数
			param <- getParam(request.uri.getQuery).liftF
			//更新命令
			cmd <-
			{
				log.info("通话事件推送更新回调")
				log.info("+---------------------------------------------------------------------------------------------------------------------------+")
				param.foreach { case (k, v) => log.info(s" $k => $v") }
				log.info("+---------------------------------------------------------------------------------------------------------------------------+")
				//Map参数构建更新命令
				Update.fromMap(param)
			}.liftF
			//电话绑定关系
			id <- bindingRelationRepository.getByPK(cmd.call).map
			{
				case Some((_, id)) => id
				//404 NotFound
				case _ => throw NotFound(s"绑定关系不存在")
			}
			//发送更新命令
			_ <- refFor(id).ask(cmd)
			//Http200响应
		} yield Restful.ok
	})

	//从QueryString获取Map集合参数
	private def getParam(query: String) = query
	.split("&")
	.map(_.split("="))
	.foldLeft(Map.empty[String, String])
	{
		case (map, Array(x, y)) => map + (x -> y)
		case (map, _)           => map
	}

	private def refFor(id: String) = registry.refFor[CallUpRecordEntity](id)

}
