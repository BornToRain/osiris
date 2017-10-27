package com.oasis.osiris.common.impl

import java.time.Instant

import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.oasis.osiris.tool.db.Implicits._
import com.oasis.osiris.tool.functional.Lift.ops._

import scala.concurrent.ExecutionContext

/**
  * 读边
  */
//通话记录仓库
class CallUpRecordRepository(session: CassandraSession)(implicit ec: ExecutionContext)
{
  def gets = for
    {
    list <- session.selectAll("SELECT * FROM call_up_record")
    data <- list.map(row =>
    {
      val id = row.getString("id")
      val call = row.getString("call")
      val called = row.getString("called")
      val maxCallTime = row.getImplicitly[Option[Long]]("max_call_time")
      val callTime = row.getImplicitly[Option[Long]]("call_time")
      val ringTime = row.getImplicitly[Option[Instant]]("ring_time")
      val beginTime = row.getImplicitly[Option[Instant]]("begin_time")
      val endTime = row.getImplicitly[Option[Instant]]("end_time")
      val recordFile = row.getImplicitly[Option[String]]("record_file")
      val fileServer = row.getImplicitly[Option[String]]("file_server")
      val noticeUri = row.getImplicitly[Option[String]]("notice_uri")
      val thirdId = row.getImplicitly[Option[String]]("third_id")
      val callId = row.getImplicitly[Option[String]]("call_id")
      val createTime = row.getTimestamp("create_time").toInstant
      val updateTime = row.getTimestamp("update_time").toInstant
      CallUpRecord(id, call, called, maxCallTime, callTime, ringTime, beginTime, endTime, recordFile, fileServer, noticeUri, thirdId, callId,
        createTime, updateTime)
    }).liftF
  } yield data
}

//绑定关系仓库
class BindingRelationRepository(session: CassandraSession)(implicit ec: ExecutionContext)
{
  def getByPK(call: String) = for
    {
    data <- session.selectOne("SELECT * FROM binding_relation WHERE call = ?", call)
    tuple <- data.map(row => (row.getString("called"), row.getString("call_up_record_id"))).liftF
  } yield tuple
}
