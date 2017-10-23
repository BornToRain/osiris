package com.oasis.osiris.common.impl

import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.oasis.osiris.common.api.CommonService
import com.oasis.osiris.common.api.MoorDTO.TestDTO
import com.oasis.osiris.common.impl.CallUpRecordCommand._
import com.oasis.osiris.tool.{Api, IdWorker, Restful}

import scala.concurrent.ExecutionContext

/**
	* 公用接口实现
	*/
class CommonServiceImpl
(
	registry: PersistentEntityRegistry,
	callUpRecordRepository: CallUpRecordRepository
)
(implicit ec: ExecutionContext) extends CommonService with Api
{

	override def getCallUpRecords = v2(ServerServiceCall
	{
		_ =>
		for
		{
			//仓库所有通话记录
			list <- callUpRecordRepository.gets
			//转成DTO
			data <- list.map(
				d => TestDTO(d.id, d.call, d.called, d.maxCallTime, d.callTime, d.beginTime, d.endTime, d.noticeUri, d.thirdId, d.createTime, d.updateTime))
			.liftF
			//Http200响应
		} yield data
	})

	override def bindCallUpRecord = v2(ServerServiceCall
	{
		(request, data) =>
		for
		{
			//Id生成器
			id <- IdWorker.liftF()
			//发出绑定命令
			_ <- refFor(id).ask(Bind(id, data.thirdId, data.call, data.called, data.maxCallTime, data.noticeUri))
			//Http201响应
			result <- Restful.created(request)(id)
		} yield result
	})

	override def hangUpCallUpRecord(id: String) = v2(ServerServiceCall
	{
		(_, _) =>
		for
		{
			//发出挂断命令
			_ <- refFor(id).ask(HangUp)
			//Http204响应
			result <- Restful.noContent
		} yield result
	})

	private def refFor(id: String) = registry.refFor[CallUpRecordEntity](id)
}
