package com.oasis.osiris.common.impl

import java.time.Instant

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.oasis.osiris.common.impl.CallEventStatus.CallEventStatus
import com.oasis.osiris.common.impl.CallStatus.CallStatus
import com.oasis.osiris.tool.DateTool
import com.oasis.osiris.tool.JSONTool._
import play.api.libs.json.{Format, Json}

/**
	* 领域命令
	*/
//通话记录命令集
sealed trait CallUpRecordCommand[Reply] extends ReplyType[Reply]

object CallUpRecordCommand
{

	//绑定命令
	case class Bind(
		id: String,
		thirdId: Option[String],
		call: String,
		called: String,
		maxCallTime: Option[Long],
		noticeUri: Option[String],
		createTime: Instant = Instant.now,
		updateTime: Instant = Instant.now)
	extends CallUpRecordCommand[String]

	object Bind
	{
		implicit val format: Format[Bind] = Json.format
	}

	//挂断命令
	case object HangUp extends CallUpRecordCommand[Done]
	{
		implicit val format: Format[HangUp.type] = singletonFormat(HangUp)
	}

	//更新命令
	case class Update
	(
		id: Option[String],
		call: String,
		called: String,
		callType: String,
		ringTime: Option[Instant],
		beginTime: Option[Instant],
		endTime: Option[Instant],
		status: CallStatus,
		eventStatus: CallEventStatus,
		recordFile: Option[String],
		fileServer: Option[String],
		callId: Option[String],
		updateTime: Instant = Instant.now
	) extends CallUpRecordCommand[Option[CallUpRecord]]

	object Update
	{
		implicit val format: Format[Update] = Json.format

		//map转case class
		def fromMap(map: Map[String, String]) =
		{
			val ringTime = map.get("Ring").map(DateTool.toInstant)
			val beginTime = map.get("Begin").map(DateTool.toInstant)
			val endTime = map.get("End").map(DateTool.toInstant)
			val status = CallStatus.withName(map("State"))
			val eventStatus = CallEventStatus.withName(map("CallState"))

			apply(map.get("ActionID"), map("CallNo"), map("CalledNo"), map("CallType"), ringTime, beginTime, endTime, status, eventStatus,
				map.get("RecordFile"),
				map.get("FileServer"), map.get("CallID"))
		}
	}

}

//短信记录命令集
sealed trait SmsRecordCommand[Reply] extends ReplyType[Reply]

object SmsRecordCommand
{

	//创建
	case class Create(
		id: String,
		mobile: String,
		smsType: String,
		isBusiness: Boolean,
		createTime: Instant = Instant.now,
		updateTime: Instant = Instant.now) extends SmsRecordCommand[String]

	object Create
	{
		implicit val format: Format[Create] = Json.format
	}

}


