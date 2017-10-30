package com.oasis.osiris.common.impl.client
import com.oasis.osiris.tool.functional.Lift.ops._

import scala.concurrent.ExecutionContext

class SmsClient(implicit ec: ExecutionContext)
{
  import com.aliyun.mns.client.CloudAccount
  import com.aliyun.mns.model.{BatchSmsAttributes, MessageAttributes, RawTopicMessage}
  import org.slf4j.LoggerFactory
  private[this] val log = LoggerFactory.getLogger(classOf[SmsClient])

  def sendPayment(code:String)(mobile:String) = send("SMS_96870043")(mobile)(Map("code" -> code))(isPromotion = false)

  /**
    * 阿里云短信SDK
    */
  private[this] def send(templateId: String)(mobile: String)(map: Map[String, String] = Map.empty)(isPromotion: Boolean = false) = for
  {
    //Step 1. 获取主题引用
    account <- new CloudAccount(SmsClient.key, SmsClient.secret, SmsClient.endPoint).liftF
    topic <- account.getMNSClient.getTopicRef(SmsClient.topic).liftF
    //Step 2. 设置SMS消息体（必须）
    // 注：目前暂时不支持消息内容为空，需要指定消息内容，不为空即可。
    msg <-
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
    result <- topic.publishMessage(msg,msgAttributes).liftF
  } yield result
}

object SmsClient
{
  import com.typesafe.config.ConfigFactory
  //阿里云key
  lazy val key      = ConfigFactory.load.getString("sms.key")
  //阿里云密钥
  lazy val secret   = ConfigFactory.load.getString("sms.secret")
  //阿里云短信网关
  lazy val endPoint = "https://1126869279253886.mns.cn-beijing.aliyuncs.com/"
  //阿里云短信主题
  lazy val topic    = "sms.topic-cn-beijing"
  //短信签名
  lazy val sign     = "泓华医疗"

  //阿里云短信模版
  object Template
  {
    //验证码模版
    lazy val captcha = "SMS_71161028"
    //支付模版
    lazy val payment = "SMS_96870043"
    //达人通知模版
    lazy val notice  = "SMS_103945002"
  }

}
