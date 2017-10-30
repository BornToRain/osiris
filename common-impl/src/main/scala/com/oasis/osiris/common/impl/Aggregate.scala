package com.oasis.osiris.common.impl

import java.time.Instant

import com.oasis.osiris.common.impl.CallType.CallType
import com.oasis.osiris.common.impl.CallUpRecordCommand.Bind
import com.oasis.osiris.common.impl.CallUpRecordEvent.Updated
import com.oasis.osiris.tool.JSONTool._
import play.api.libs.json.{Format, Json}

/**
  * 领域聚合根
  */
//通话记录聚合根
case class CallUpRecord
(
  id         : String,
  //呼叫
  call       : String,
  //被叫
  called     : String,
  //最大通话时长
  maxCallTime: Option[Int],
  //本次通话时长
  callTime   : Option[Int],
  //通话类型
  callType   : Option[CallType],
  //通话振铃时间
  ringTime   : Option[Instant],
  //接通时间
  beginTime  : Option[Instant],
  //挂断时间
  endTime    : Option[Instant],
  //通话录音文件名
  recordFile : Option[String],
  //录音文件存在服务器
  fileServer : Option[String],
  //通知地址
  noticeUri  : Option[String],
  //第三方唯一标识
  thirdId    : Option[String],
  //容联七陌唯一标识
  callId     : Option[String],
  //通话状态
//  status:CallStatus,
//  eventStatus:CallEventStatus,
  //通话事件状态
  createTime : Instant,
  updateTime : Instant
)
{
  //更新聚合根
  def update(event: Updated) = copy(
    ringTime = event.cmd.ringTime,
    beginTime = event.cmd.beginTime,
    endTime = event.cmd.endTime,
    recordFile = event.cmd.recordFile,
    fileServer = event.cmd.fileServer,
    callId = event.cmd.callId,
    updateTime = event.cmd.updateTime
  )
}

object CallUpRecord
{
  implicit val format: Format[CallUpRecord] = Json.format

  //绑定电话关系
  def bind(cmd: Bind) = CallUpRecord(cmd.id, cmd.call, cmd.called, cmd.maxCallTime, None, None, None, None, None, None, None, cmd.noticeUri, cmd
  .thirdId,
    None,
    cmd.createTime, cmd.updateTime)
}

//通话类型
object CallType extends Enumeration
{
  type CallType = Value
  val dialout      = Value("外呼通话")
  val normal       = Value("普通来电")
  val transfer     = Value("转接电话")
  val dialTransfer = Value("外呼转接")
  implicit val format: Format[CallType] = enumFormat(CallType)
}

//短信记录聚合根
case class SmsRecord
(
  id        : String,
  //短信在阿里唯一标识
  messageId : Option[String],
  //发送手机号
  mobile    : String,
  //短信类型
  smsType   : String,
  //短信接收成功
  isSuccess : Boolean,
  createTime: Instant,
  updateTime: Instant
)

object SmsRecord
{
  implicit val format: Format[SmsRecord] = Json.format
}

//短信类型
object SmsType extends Enumeration
{
  type SmsType = Value
  val login      = Value("登录")
  val register   = Value("注册")
  val invitation = Value("邀请")
  val payment    = Value("支付")
  val notice     = Value("达人通知")
  implicit val format: Format[SmsType] = enumFormat(SmsType)
}