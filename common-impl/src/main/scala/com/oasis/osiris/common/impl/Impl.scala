package com.oasis.osiris.common.impl

import com.lightbend.lagom.scaladsl.api.transport.NotFound
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import com.oasis.osiris.common.api.CommonService
import com.oasis.osiris.common.api.MoorDTO.CallEventStatus.CallEventStatus
import com.oasis.osiris.common.api.MoorDTO.CallStatus.CallStatus
import com.oasis.osiris.common.impl.CallUpRecordCommand._
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

	override def calledCallUpRecord(mobile: String) = v2(ServerServiceCall
	{
		_ =>
		log.info(s"容联七陌电话回调 =====> $mobile")
		bindingRelationRepository.getByPK(mobile)
		.map
		{
			case Some(d) => d
			//404 NotFound
			case _ => throw NotFound(s"手机号${mobile }绑定关系不存在")
		}
	})

	override def callbackCallUpRecord = v2(ServerServiceCall
	{
		(request,_) =>
		val s = request.uri.getQuery.split("&")

			log.error(request.uri.getQuery)
			Restful.noContent
	})

//	override def callbackCallUpRecord(CallNo: String, CalledNo: String, CallSheetID: String,
//		CallType: String, Ring: String, Begin: String, End: String,
//		QueueTime: String, Agent: String, Exten: String,
//		AgentName: String, Queue: String, State: CallStatus,
//		CallState: CallEventStatus, ActionID: String,
//		WebcallActionID: String, RecordFile: String, FileServer: String,
//		Province: String, District: String, CallID: String,
//		IVRKEY: String, AccountId: String, AccountName: String) = v2(ServerServiceCall
//	{
//		(_, _) =>
//		log.info("通话事件推送更新回调")
//		log.info("+---------------------------------------------------------------------------------------------------------------------------+")
//		log.info(" CallNo => {}", CallNo)
//		log.info(" CalledNo => {}", CalledNo)
//		log.info(" CallSheetId => {}", CallSheetID)
//		log.info(" CallType => {}", CallType)
//		log.info(" RingTime => {}", Ring)
//		log.info(" BeginTime => {}", Begin)
//		log.info(" EndTime => {}", End)
//		log.info(" Queue => {}", Queue)
//		log.info(" QueueTime => {}", QueueTime)
//		log.info(" Agent => {}", Agent)
//		log.info(" AgentName => {}", AgentName)
//		log.info(" Exten => {}", Exten)
//		log.info(" State => {}", CallStatus)
//		log.info(" CallState => {}", CallEventStatus)
//		log.info(" ActionId => {}", ActionID)
//		log.info(" WebCallActionId => {}", WebcallActionID)
//		log.info(" RecordFile => {}", RecordFile)
//		log.info(" FileServer => {}", FileServer)
//		log.info(" Province => {}", Province)
//		log.info(" District => {}", District)
//		log.info(" CallId => {}", CallID)
//		log.info(" IVRkey => {}", IVRKEY)
//		log.info(" AccountId => {}", AccountId)
//		log.info(" AccountName => {}", AccountName)
//		log.info("+---------------------------------------------------------------------------------------------------------------------------+")
//		Restful.noContent
//	})

	private def refFor(id: String) = registry.refFor[CallUpRecordEntity](id)
}

object test extends App
{
	val s = """CallNo=13589771577&CallSheetID=2966c9e8-8066-4b91-93a9-c43c9f2f6036&CalledNo=01050854063&CallID=cc-ali-9-1434111238.12986&CallType=normal&RecordFile=monitor/cc.ali.1.9/20150612/20150612-201434_N00000000605_10001092_13589771577_01050854063_10001092_cc-ali-9-1434111238.12986.mp3&Ring=2015-06-12 20:13:58&Begin=2015-06-12 20:14:50&End=2015-06-12 20:15:42&QueueTime=2015-06-12 20:14:34&Queue=其他&Agent=8007&Exten=8007&AgentName=郭小芳&ActionID=&CallState=Unlink&State=dealing&FileServer=http://121.40.138.123&RingTime=1434111274.035155&IVRKEY=10004@0&Province=山东省&District=烟台市"""

//	s.split("&").foreach(println)

	val ss = s.split("&").map(s =>
	{

		val array = s.split("=")
		array.foreach(println)
		"test"
	})

	println(ss)

//	val str = "1,122,xxx,shandongyin"
//	val file=sc.textFile(logFile)
//	file.flatMap(line=>line.split(",")(3))



//	val ss = s.split("&")
//	ss.map(s => s.split("=")).reduce((x,y) => x)

//	s.groupBy(d => d == "&").foreach(println)
}
