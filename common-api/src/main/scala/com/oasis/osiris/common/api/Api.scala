package com.oasis.osiris.common.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

/**
	* 公用接口定义
	*/
trait CommonService extends Service
{
	def bindCallUpRecord: ServiceCall[MoorDTO.Binding, Done]
	def hangUpCallUpRecord(id: String): ServiceCall[NotUsed, Done]
	def calledCallUpRecord(mobile: String): ServiceCall[NotUsed, String]
	def callbackCallUpRecord:ServiceCall[NotUsed,Done]

	import Service._

	def descriptor = named("commons").withCalls(
		//临时绑定电话关系
		restCall(Method.POST, "/commons/7moor/binding", bindCallUpRecord),
		//电话挂断
		restCall(Method.DELETE, "/commons/7moor/hang-up/:id", hangUpCallUpRecord _),
		//容联七陌回调 A=>坐席=>接口=>B
		restCall(Method.GET, "/commons/7moor?mobile", calledCallUpRecord _),
		//容联七陌通话事件推送更新回调
		restCall(Method.GET,"/commons/7moor/hang-up",callbackCallUpRecord)
	).withAutoAcl(true)
}
