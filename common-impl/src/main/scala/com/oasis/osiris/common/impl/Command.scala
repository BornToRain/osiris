package com.oasis.osiris.common.impl

import java.time.Instant

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import play.api.libs.json.{Format, Json}
import com.oasis.osiris.tool.JSONTool._

/**
	* 领域命令
	*/
//通话记录命令集
sealed trait CallUpRecordCommand[Reply] extends ReplyType[Reply]

object CallUpRecordCommand
{

	//绑定命令
	case class Bind(id: String, thirdId: Option[String], call: String, called: String, maxCallTime: Option[Long], noticeUri: Option[String],
		createTime: Instant = Instant.now, updateTime: Instant = Instant.now)
	extends CallUpRecordCommand[Done]

	object Bind
	{
		implicit val format: Format[Bind] = Json.format
	}

	//挂断命令
	case object HangUp extends CallUpRecordCommand[Done]
	{
		implicit val format:Format[HangUp.type ] = singletonFormat(HangUp)
	}

}
