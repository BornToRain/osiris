package com.oasis.osiris.common.impl

import java.time.Instant

import com.oasis.osiris.common.impl.CallUpRecordCommand.Bind
import play.api.libs.json.{Format, Json}

/**
	* 领域模型
	*/
//通话记录领域模型
case class CallUpRecord
(
	id: String,
	//呼叫
	call: String,
	//被叫
	called: String,
	//最大通话时长
	maxCallTime: Option[Long],
	//本次通话时长
	callTime: Option[Long],
	//通话类型
	//通话振铃时间
	ringTime: Option[Instant],
	//接通时间
	beginTime: Option[Instant],
	//挂断时间
	endTime: Option[Instant],
	//通话录音文件名
	recordFile: Option[String],
	//录音文件存在服务器
	fileServer: Option[String],
	//通知地址
	noticeUri: Option[String],
	//第三方唯一标识
	thirdId: Option[String],
	//容联七陌唯一标识
	callId: Option[String],
	createTime: Instant,
	updateTime: Instant
////通话类型
//private CallType       callType;
)

object CallUpRecord
{
	implicit val format: Format[CallUpRecord] = Json.format

	//绑定电话关系
	def bind(cmd: Bind) = CallUpRecord(cmd.id, cmd.call, cmd.called, cmd.maxCallTime, None, None, None, None, None, None, cmd.noticeUri, cmd.thirdId,
		None,
		cmd.createTime, cmd.updateTime)
}