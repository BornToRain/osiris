package com.oasis.osiris.common.impl

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

/**
	* 序列化
	*/
object SerializerRegistry extends JsonSerializerRegistry
{
	def serializers = Vector(
		//命令
		JsonSerializer[CallUpRecordCommand.Bind],
		JsonSerializer[CallUpRecordCommand.HangUp.type],
		JsonSerializer[CallUpRecordCommand.Update],
		//事件
		JsonSerializer[CallUpRecordEvent.Bound],
		JsonSerializer[CallUpRecordEvent.HungUp.type],
		JsonSerializer[CallUpRecordEvent.Updated],
		//状态
		JsonSerializer[CallUpRecordState]
	)
}
