package com.oasis.osiris.common.impl

import java.time.Instant
import java.util.Date

import akka.Done
import akka.actor.ActorSystem
import akka.event.slf4j.SLF4JLogging
import com.datastax.driver.core.PreparedStatement
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, ReadSideProcessor}
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.oasis.osiris.common.impl.client.RedisClient
import com.oasis.osiris.tool.db.{IntCodec, OptionCodec}
import com.oasis.osiris.tool.db.Implicits._

import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * 领域事件处理
  */
//通话记录领域事件处理
class CallUpRecordEventProcessor
(
  session: CassandraSession,
  readSide: CassandraReadSide,
  actorSystem: ActorSystem
)(implicit ec: ExecutionContext) extends ReadSideProcessor[CallUpRecordEvent] with SLF4JLogging
{
  import com.oasis.osiris.common.impl.CallUpRecordEvent._

  private[this] val bindCallUpRecordPro = Promise[PreparedStatement]
  private[this] val updateCallUpRecordPro = Promise[PreparedStatement]

  //当前集群
  private[this] def getCluster = session.underlying.map(_.getCluster)

  //编码器列表
  private[this] val codecs = Seq(OptionCodec(IntCodec), OptionCodec[String], OptionCodec[Instant])

  override def aggregateTags = CallUpRecordEvent.tag.allTags

  override def buildHandler = readSide.builder[CallUpRecordEvent]("callUpRecordEventOffSet")
  .setGlobalPrepare(() => createTable)
  .setPrepare(_ => prepare)
  .setEventHandler[Bound](bind)
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
        | create_time timestamp,
        | update_time timestamp
        | )
      """.stripMargin
    }
  } yield Done

  private[this] def prepare =
  {
    //设置集群编码器
    getCluster.map(_.getConfiguration.getCodecRegistry.register(codecs: _*))
    //绑定通话记录SQL
    bindCallUpRecordPro.completeWith(session
    .prepare("""INSERT INTO call_up_record(id,call,called,max_call_time,call_type,notice_uri,third_id,create_time,update_time) VALUES(?,?,?,?,?,?,?,?,?)"""))
    //更新通话记录SQL
    updateCallUpRecordPro.completeWith(session.prepare(
      """
        |UPDATE call_up_record
        |SET
        |
      """.stripMargin))
    Future(Done)
  }

  //绑定电话关系
  private[this] def bind(event: EventStreamElement[Bound]) =
  {
    import java.time.Duration
    log.info("持久化电话绑定关系到读边")
    val cmd = event.event.cmd
    for
    {
      //redis客户端
      redis <- RedisClient(actorSystem).client
      //绑定关系存入Redis 30分钟
      _ <- redis.set[BindingRelation]("binding=>"+cmd.call,BindingRelation(cmd.id,cmd.call,cmd.called,cmd.thirdId),Some(Duration.ofMinutes(30L).getSeconds))
      //通话记录插入
      data <- bindCallUpRecordPro.future.map
      {
        d =>
        val data = d.bind
        data.setString("id", cmd.id)
        data.setString("call", cmd.call)
        data.setString("called", cmd.called)
        data.setImplicitly("max_call_time", cmd.maxCallTime)
        data.setImplicitly("notice_uri", cmd.noticeUri)
        data.setImplicitly("third_id", cmd.thirdId)
        data.setTimestamp("create_time", Date.from(cmd.createTime))
        data.setTimestamp("update_time", Date.from(cmd.updateTime))
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
    //绑定通话记录SQL
    insertSmsRecordPro.completeWith(session.prepare(
      """INSERT INTO sms_record(id,message_id,mobile,sms_type,is_success,
        |create_time,update_time) VALUES(?,?,?,?,?,?,?)""".stripMargin))
    //绑定关系SQL => 30min存活时间
    Future(Done)
  }

  //创建短信记录
  private[this] def create(event: EventStreamElement[Created]) =
  {
    log.info("持久化短信记录到读边")
    val cmd = event.event.cmd
    for
      {
      a <- insertSmsRecordPro.future.map
      {
        ps =>
        val d = ps.bind
          val d1:Option[String] = Some("123")
        d.setString("id", cmd.id)
        d.setImplicitly("message_id",d1 )
        d.setString("mobile", cmd.mobile)
        d.setString("sms_type", cmd.smsType)
        d.setBool("is_success", cmd.isSuccess)
        d.setTimestamp("create_time", Date.from(cmd.createTime))
        d.setTimestamp("update_time", Date.from(cmd.updateTime))
      }
    } yield Vector(a)
  }
}
