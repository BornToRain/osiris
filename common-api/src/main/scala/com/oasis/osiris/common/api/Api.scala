package com.oasis.osiris.common.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import com.oasis.osiris.common.api.MoorDTO.TestDTO

/**
	* 公用接口定义
	*/
trait CommonService extends Service
{
	def getCallUpRecords: ServiceCall[NotUsed, Seq[TestDTO]]
	def bindCallUpRecord: ServiceCall[MoorDTO.Binding, Done]
	def hangUpCallUpRecord(id:String):ServiceCall[NotUsed,Done]

	import Service._

	def descriptor = named("commons").withCalls(
		//全部通话记录
		restCall(Method.GET, "/commons/7moor", getCallUpRecords),
		//临时绑定电话关系
		restCall(Method.POST, "/commons/7moor/binding", bindCallUpRecord),
		//电话挂断
		restCall(Method.DELETE,"/commons/7moor/hang-up/:id",hangUpCallUpRecord _)
	).withAutoAcl(true)
}
