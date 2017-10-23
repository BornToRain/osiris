package com.oasis.osiris.common.api

import java.time.Instant

import play.api.libs.json.{Format, Json}
import com.oasis.osiris.tool.JSONTool._

/**
	* 接口层DTO对象
	*/
//容联七陌DTO
object MoorDTO
{

	//绑定关系
	case class Binding(thirdId: Option[String], call: String, called: String, maxCallTime: Option[Long], noticeUri: Option[String])

	object Binding
	{
		implicit val format: Format[Binding] = Json.format
	}

	//通话记录领域模型
	case class TestDTO
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
		//接通时间
		beginTime: Option[Instant],
		//挂断时间
		endTime: Option[Instant],
		//通知地址
		noticeUri: Option[String],
		//第三方唯一标识
		thirdId: Option[String],
		createTime: Instant,
		updateTime: Instant
	)

	object TestDTO
	{
		implicit val format: Format[TestDTO] = Json.format
	}

}
