package com.oasis.osiris.common.impl

import com.oasis.osiris.common.impl.CallEventStatus.CallEventStatus
import com.oasis.osiris.common.impl.CallStatus.CallStatus
import com.oasis.osiris.common.impl.CallUpRecordEvent.Updated
import com.oasis.osiris.tool.JSONTool._
import play.api.libs.json.{Format, Json}

/**
  * 领域状态
  */
//通话状态
object CallStatus extends Enumeration
{
  type CallStatus = Value
  //已接、振铃未接听、ivr放弃、排队放弃、黑名单、留言
  val dealing, notDeal, leak, queueLeak, blackList, voicemail = Value
  implicit val format: Format[CallStatus] = enumFormat(CallStatus)
}

//通话事件状态
object CallEventStatus extends Enumeration
{
  type CallEventStatus = Value
  val Ring, Ringing, Link, Hangup, Unlink = Value
  implicit val format: Format[CallEventStatus] = enumFormat(CallEventStatus)
}

//通话记录状态
case class CallUpRecordState
(
  data       : Option[CallUpRecord],
  status     : Option[CallStatus],
  eventStatus: Option[CallEventStatus]
)
{
  //更新通话记录状态
  def update(event: Updated) = copy(
    status = Some(event.cmd.status),
    eventStatus = Some(event.cmd.eventStatus),
    data = this.data.map(_.update(event))
  )
}

object CallUpRecordState
{
  implicit val format: Format[CallUpRecordState] = Json.format
  //不存在
  lazy     val nonexistence                      = CallUpRecordState(None, None, None)
}
