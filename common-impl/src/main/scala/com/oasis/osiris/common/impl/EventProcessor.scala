package com.oasis.osiris.common.impl

import java.time.Instant
import java.util.Date

import akka.Done
import akka.event.slf4j.SLF4JLogging
import com.datastax.driver.core.PreparedStatement
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, ReadSideProcessor}
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.oasis.osiris.tool.db.{IntCodec, OptionCodec}
import com.oasis.osiris.tool.db.Implicits._

import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * 领域事件处理
  */
//通话记录领域事件处理
class CallUpRecordEventProcessor
(
  session : CassandraSession,
  readSide: CassandraReadSide
)(implicit ec: ExecutionContext) extends ReadSideProcessor[CallUpRecordEvent] with SLF4JLogging
{
  import com.oasis.osiris.common.impl.CallUpRecordEvent._

  private[this] val bindCallUpRecordPro   = Promise[PreparedStatement]
  private[this] val updateCallUpRecordPro = Promise[PreparedStatement]

  //当前集群
  private[this] def getCluster = session.underlying.map(_.getCluster)

  //编码器列表
  private[this] val codecs = Seq(OptionCodec(IntCodec), OptionCodec[String], OptionCodec[Instant])

  override def aggregateTags = CallUpRecordEvent.tag.allTags

  override def buildHandler = readSide.builder[CallUpRecordEvent]("callUpRecordEventOffSet")
  .setGlobalPrepare(() => createTable)
  .setPrepare(_ => prepare)
  .setEventHandler[Bound](bound)
  .setEventHandler[Updated](updated)
  .build

  //数据库表创建
  private[this] def createTable = for
    {
    //通话记录表
    _ <- session.executeCreateTable
    {
      """
        |CREATE TABLE IF NOT EXISTS call_up_record
        |(
        | id text PRIMARY KEY,
        | call text,
        | called text,
        | max_call_time int,
        | call_time int,
        | call_type text,
        | ring_time timestamp,
        | begin_time timestamp,
        | end_time timestamp,
        | record_file text,
        | file_server text,
        | notice_uri text,
        | third_id text,
        | call_id text,
        | status text,
        | event_status text,
        | create_time timestamp,
        | update_time timestamp
        |)
      """.stripMargin
    }
  } yield Done

  private[this] def prepare =
  {
    //设置集群编码器
    getCluster.map(_.getConfiguration.getCodecRegistry.register(codecs: _*))
    //绑定通话记录SQL
    bindCallUpRecordPro.completeWith(session
    .prepare(
      """INSERT INTO call_up_record(id,call,called,max_call_time,call_type,notice_uri,third_id,create_time,update_time) VALUES(?,?,?,?,?,?,?,?,?)"""))
    //更新通话记录SQL
    updateCallUpRecordPro.completeWith(session.prepare(
      """
        |UPDATE call_up_record
        |SET
        | call_type = ?,
        | ring_time = ?,
        | begin_time = ?,
        | end_time = ?,
        | record_file = ?,
        | file_server = ?,
        | third_id = ?,
        | call_id = ?,
        | status = ?,
        | event_status = ?,
        | update_time = ?
        |WHERE id = ?
      """.stripMargin))
    Future(Done)
  }

  //绑定电话关系
  private[this] def bound(event: EventStreamElement[Bound]) =
  {
    log.info("持久化电话绑定关系到读边")
    val cmd = event.event.cmd
    for
      {
      //通话记录插入
      data <- bindCallUpRecordPro.future.map
      {
        ps =>
        val d = ps.bind
        d.setString("id", cmd.id)
        d.setString("call", cmd.call)
        d.setString("called", cmd.called)
        d.setImplicitly("max_call_time", cmd.maxCallTime)
        d.setImplicitly("notice_uri", cmd.noticeUri)
        d.setImplicitly("third_id", cmd.thirdId)
        d.setTimestamp("create_time", Date.from(cmd.createTime))
        d.setTimestamp("update_time", Date.from(cmd.updateTime))
      }
    } yield Vector(data)
  }

  //更新通话记录
  private[this] def updated(event: EventStreamElement[Updated]) =
  {
    log.info("更新通话记录")
    val cmd = event.event.cmd
    for
      {
      //通话记录更新
      data <- updateCallUpRecordPro.future.map
      {
        ps =>
        val d = ps.bind
        d.setString("call_type", cmd.callType.toString)
        d.setImplicitly("ring_time", cmd.ringTime)
        d.setImplicitly("begin_time", cmd.beginTime)
        d.setImplicitly("end_time", cmd.endTime)
        d.setImplicitly("record_file", cmd.recordFile)
        d.setImplicitly("file_server", cmd.fileServer)
        d.setImplicitly("call_id", cmd.callId)
        d.setString("status", cmd.status.toString)
        d.setString("event_status", cmd.eventStatus.toString)
        d.setTimestamp("update_time", Date.from(cmd.updateTime))
        d.setString("id", event.entityId)
      }
    } yield Vector(data)
  }
}

//短信记录领域事件处理
class SmsRecordEventProcessor
(
  session : CassandraSession,
  readSide: CassandraReadSide
)(implicit ec: ExecutionContext) extends ReadSideProcessor[SmsRecordEvent] with SLF4JLogging
{
  import com.oasis.osiris.common.impl.SmsRecordEvent._

  private[this] val insertSmsRecordPro = Promise[PreparedStatement]

  override def aggregateTags = SmsRecordEvent.tag.allTags

  override def buildHandler = readSide.builder[SmsRecordEvent]("smsRecordEventOffSet")
  .setGlobalPrepare(() => createTable)
  .setPrepare(_ => prepare)
  .setEventHandler[Created](create)
  .build

  private[this] def createTable = for
  {
    //短信记录表
    _ <- session.executeCreateTable
    {
      """
        |CREATE TABLE IF NOT EXISTS sms_record
        |(
        | id text PRIMARY KEY,
        | message_id text,
        | mobile text,
        | sms_type text,
        | is_success boolean,
        | create_time timestamp,
        | update_time timestamp
        |)
      """.stripMargin
    }
  } yield Done

  private[this] def prepare =
  {
    //短信记录SQL
    insertSmsRecordPro.completeWith(session.prepare(
      """INSERT INTO sms_record(id,message_id,mobile,sms_type,is_success,
        |create_time,update_time) VALUES(?,?,?,?,?,?,?)""".stripMargin))
    Future(Done)
  }

  //创建短信记录
  private[this] def create(event: EventStreamElement[Created]) =
  {
    log.info("持久化短信记录到读边")
    val cmd = event.event.cmd
    for
    {
      data <- insertSmsRecordPro.future.map
      {
        ps =>
        val d = ps.bind
        d.setString("id", cmd.id)
        d.setString("message_id", cmd.messageId)
        d.setString("mobile", cmd.mobile)
        d.setString("sms_type", cmd.smsType.toString)
        d.setBool("is_success", cmd.isSuccess)
        d.setTimestamp("create_time", Date.from(cmd.createTime))
        d.setTimestamp("update_time", Date.from(cmd.updateTime))
      }
    } yield Vector(data)
  }
}
