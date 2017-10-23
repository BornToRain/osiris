package com.oasis.osiris.common.impl

import java.time.Instant
import java.util.Date

import akka.Done
import akka.event.slf4j.SLF4JLogging
import com.datastax.driver.core.{PreparedStatement, TypeCodec}
import com.datastax.driver.extras.codecs.joda.InstantCodec
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, ReadSideProcessor}
import com.oasis.osiris.common.impl.CallUpRecordEvent._
import com.oasis.osiris.tool.db.Implicits._
import com.oasis.osiris.tool.db.{LongCodec, OptionCodec}

import scala.concurrent.{ExecutionContext, Future, Promise}

/**
	* 领域事件处理
	*/
//通话记录领域事件处理
class CallUpRecordEventProcessor(session: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext)
extends ReadSideProcessor[CallUpRecordEvent] with SLF4JLogging
{
	private val bindPro = Promise[PreparedStatement]

	override def aggregateTags = CallUpRecordEvent.tag.allTags
	override def buildHandler = readSide.builder[CallUpRecordEvent]("callUpRecordEventOffSet")
	.setGlobalPrepare(() => createTable)
	.setPrepare(_ => prepare)
	.setEventHandler[Bound](bind)
	.build

	//数据库表创建
	private def createTable = for
		{
		_ <- session.executeCreateTable
		{
			"""
			CREATE TABLE IF NOT EXISTS call_up_record
	    (
		    id text PRIMARY KEY,
				call text,
				called text,
				max_call_time bigint,
				call_time bigint,
				ring_time timestamp,
				begin_time timestamp,
				end_time timestamp,
				record_file text,
				file_server text,
				notice_uri text,
				third_id text,
				call_id text,
				create_time timestamp,
				update_time timestamp
	    )
			"""
		}
	} yield Done

	//SQLs
	private def prepare =
	{
		//设置集群编码器
		getCluster.map(_.getConfiguration.getCodecRegistry.register(codecs:_*))
		//绑定关系SQL
		bindPro.completeWith(session.prepare("INSERT INTO call_up_record(id,call,called,max_call_time,notice_uri,third_id,create_time,update_time) VALUES(?,?,?,?,?,?,?," + "?)"))

		Future(Done)
	}

	//当前集群
	private def getCluster = session.underlying.map(_.getCluster)

	//编码器列表
	private val codecs = Seq(OptionCodec(LongCodec),OptionCodec[String],OptionCodec[Instant])

	//绑定电话关系
	private def bind(event: EventStreamElement[Bound]) =
	{
		log.info("持久化绑定绑定到读边")
		val cmd = event.event.cmd
		bindPro.future.map(ps =>
		{
			val data = ps.bind
			data.setString("id", cmd.id)
			data.setString("call", cmd.call)
			data.setString("called", cmd.called)
			data.setImplicitly("max_call_time", cmd.maxCallTime)
			data.setImplicitly("notice_uri", cmd.noticeUri)
			data.setImplicitly("third_id", cmd.thirdId)
			data.setTimestamp("create_time",Date.from(cmd.createTime))
			data.setTimestamp("update_time",Date.from(cmd.updateTime))

			Vector(data)
		})
	}
}
