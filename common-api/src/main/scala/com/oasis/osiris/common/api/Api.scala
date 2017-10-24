package com.oasis.osiris.common.api

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import com.oasis.osiris.common.api.MoorDTO.CallEventStatus.CallEventStatus
import com.oasis.osiris.common.api.MoorDTO.CallStatus.CallStatus

/**
	* 公用接口定义
	*/
trait CommonService extends Service
{
	def bindCallUpRecord: ServiceCall[MoorDTO.Binding, Done]
	def hangUpCallUpRecord(id: String): ServiceCall[NotUsed, Done]
	def calledCallUpRecord(mobile: String): ServiceCall[NotUsed, String]
	def callbackCallUpRecord:ServiceCall[NotUsed,Done]
//	def callbackCallUpRecord(CallNo: String, CalledNo: String, CallSheetID: String, CallType: String,
//		Ring: String, Begin: String, End: String, QueueTime: String, Agent: String, Exten: String, AgentName: String,
//		Queue: String, State: CallStatus, CallState: CallEventStatus, ActionID: String, WebcallActionID: String,
//		RecordFile: String, FileServer: String, Province: String, District: String, CallID: String, IVRKEY: String,
//		AccountId: String, AccountName: String): ServiceCall[NotUsed, Done]

	import Service._

	def descriptor = named("commons").withCalls(
		//临时绑定电话关系
		restCall(Method.POST, "/commons/7moor/binding", bindCallUpRecord),
		//电话挂断
		restCall(Method.DELETE, "/commons/7moor/hang-up/:id", hangUpCallUpRecord _),
		//容联七陌回调 A=>坐席=>接口=>B
		restCall(Method.GET, "/commons/7moor?mobile", calledCallUpRecord _),
		//容联七陌通话事件推送更新回调
		restCall(Method.GET,"/commons/7moor/hang-up",callbackCallUpRecord _)
//		restCall(Method.GET,
//			"""/commons/7moor/hang-up?CallNo&CalledNo&CallSheetID&CallType&Ring&Begin&End&
//				|QueueTime&Agent&Exten&AgentName&Queue&State&CallState&ActionID&WebcallActionID&
//				|RecordFile&FileServer&Province&District&CallID&IVRKEY&AccountId&AccountName""".stripMargin,
//			callbackCallUpRecord _)
	).withAutoAcl(true)
}
