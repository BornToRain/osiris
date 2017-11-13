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
    xs   <- session.selectAll("SELECT * FROM call_up_record")
    data <- xs.map(row =>
    {
      import com.oasis.osiris.common.impl.CallEventStatus.CallEventStatus
      import com.oasis.osiris.common.impl.CallStatus.CallStatus
      import com.oasis.osiris.common.impl.CallType.CallType
      val id = row.getString("id")
      val call = row.getString("call")
      val called = row.getString("called")
      val maxCallTime = row.getImplicitly[Option[Int]]("max_call_time")
      val callTime = row.getImplicitly[Option[Int]]("call_time")
      val callType = row.getImplicitly[Option[CallType]]("call_type")
      val ringTime = row.getImplicitly[Option[Instant]]("ring_time")
      val beginTime = row.getImplicitly[Option[Instant]]("begin_time")
      val endTime = row.getImplicitly[Option[Instant]]("end_time")
      val recordFile = row.getImplicitly[Option[String]]("record_file")
      val fileServer = row.getImplicitly[Option[String]]("file_server")
      val noticeUri = row.getImplicitly[Option[String]]("notice_uri")
      val thirdId = row.getImplicitly[Option[String]]("third_id")
      val callId = row.getImplicitly[Option[String]]("call_id")
      val status = row.getImplicitly[Option[CallStatus]]("status")
      val eventStatus = row.getImplicitly[Option[CallEventStatus]]("event_status")
      val createTime = row.getTimestamp("create_time").toInstant
      val updateTime = row.getTimestamp("update_time").toInstant
      CallUpRecord(id, call, called, maxCallTime, callTime, callType, ringTime, beginTime, endTime, recordFile, fileServer, noticeUri, thirdId,
        callId, status, eventStatus
        , createTime, updateTime)
    }).liftF
  } yield data
}
