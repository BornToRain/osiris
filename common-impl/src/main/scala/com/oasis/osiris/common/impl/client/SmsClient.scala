package com.oasis.osiris.common.impl.client
import com.oasis.osiris.tool.functional.Lift.ops._

import scala.concurrent.ExecutionContext

class SmsClient(implicit ec: ExecutionContext)
{
  import com.aliyun.mns.client.CloudAccount
  import com.aliyun.mns.model.{BatchSmsAttributes, MessageAttributes, RawTopicMessage}
  import com.oasis.osiris.common.impl.client.SmsClient.Template

  import scala.concurrent.Future

  //达人通知短信
  def sendNotice(mobile: String) = sendSms(mobile)(Template.notice)

  //支付短信
  def sendPayment(mobile: String)(code: String) = sendCaptcha(mobile)(code)(Template.payment)

  //身份验证短信
  def sendAuthentication(mobile: String)(code: String) = sendCaptcha(mobile)(code)(Template.authentication)

  //发送有验证码短信
  private[this] def sendCaptcha(mobile: String)(code: String)(templateId: String) = for
  {
    f      <- (send(mobile)(templateId)_).liftF
    result <- templateId match
    {
      //身份验证
      case Template.authentication => f(Map("code" -> code, "product" -> SmsClient.sign))(false)
      //支付
      case Template.payment => f(Map("code" -> code))(false)
    }
  } yield result

  //发送无验证码短信
  private[this] def sendSms(mobile: String)(templateId: String) = for
  {
    f      <- (send(mobile)(templateId)_).liftF
    result <- templateId match
    {
      //达人通知
      case Template.notice => f(Map.empty)(false)
    }
  } yield result

  /**
    * 阿里云短信SDK发送短信
    */
  private[this] def send(mobile: String)(templateId: String)(map: Map[String, String] = Map.empty)(isPromotion: Boolean = false): Future[String] = for
  {
    //Step 1. 获取主题引用
    account       <- new CloudAccount(SmsClient.key, SmsClient.secret, SmsClient.endPoint).liftF
    topic         <- account.getMNSClient.getTopicRef(SmsClient.topic).liftF
    //Step 2. 设置SMS消息体（必须）
    // 注：目前暂时不支持消息内容为空，需要指定消息内容，不为空即可。
    msg           <-
    {
      val msg = new RawTopicMessage
      msg.setMessageBody("sms-message")
      msg
    }.liftF
    //Step 3. 生成SMS消息属性
    msgAttributes <-
    {
      val batchSmsAttributes = new BatchSmsAttributes
      // 3.1 设置发送短信的签名（SMSSignName） 推广短信不能带签名
      if (!isPromotion) batchSmsAttributes.setFreeSignName(SmsClient.sign)
      // 3.2 设置发送短信使用的模板（SMSTempateCode）
      batchSmsAttributes.setTemplateCode(templateId)
      // 3.3 设置发送短信所使用的模板中参数对应的值（在短信模板中定义的，没有可以不用设置）
      val smsReceiverParams = new BatchSmsAttributes.SmsReceiverParams()
      map.foreach(d => smsReceiverParams.setParam(d._1, d._2))
      // 3.4 增加接收短信的号码
      batchSmsAttributes.addSmsReceiver(mobile, smsReceiverParams)
      val msgAttributes = new MessageAttributes
      msgAttributes.setBatchSmsAttributes(batchSmsAttributes)
      msgAttributes
    }.liftF
    //Step 4. 发布SMS消息
    result        <- topic.publishMessage(msg, msgAttributes).getMessageId.liftF
  } yield result
}

object SmsClient
{
  import com.typesafe.config.ConfigFactory

  private[this] val config   = ConfigFactory.load
  //阿里云key
  private[impl] val key      = config.getString("sms.key")
  //阿里云密钥
  private[impl] val secret   = config.getString("sms.secret")
  //阿里云短信网关
  private[impl] val endPoint = "https://1126869279253886.mns.cn-beijing.aliyuncs.com/"
  //阿里云短信主题
  private[impl] val topic    = "sms.topic-cn-beijing"
  //短信签名
  private[impl] val sign     = "泓华医疗"

  //阿里云短信模版
  object Template
  {
    //验证码模版
    private[impl] val authentication = "SMS_71161028"
    //支付模版
    private[impl] val payment        = "SMS_96870043"
    //达人通知模版
    private[impl] val notice         = "SMS_103945002"
  }

}
