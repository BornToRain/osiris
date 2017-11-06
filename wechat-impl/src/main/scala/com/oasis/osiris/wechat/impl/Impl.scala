package com.oasis.osiris.wechat.impl
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.oasis.osiris.tool.Api
import com.oasis.osiris.wechat.api.WechatService
import com.oasis.osiris.wechat.impl.client.WechatClient

import scala.concurrent.ExecutionContext

/**
  * 微信接口实现
  */
class WechatServiceImpl
(
  registry: PersistentEntityRegistry,
  wechat  : WechatClient
)(implicit ec: ExecutionContext) extends WechatService with Api
{

  import akka.Done
  import com.lightbend.lagom.scaladsl.server.ServerServiceCall

  import scala.concurrent.Future
  override def get(signature: String, timestamp: String, nonce: String,
    echostr: String) = v2(ServerServiceCall
  {
    _ =>
    wechat.get("cgi-bin/token")("grant_type=client_credential", "appid=wx5016abe985f98063", "secret=87772d5c80266ebd36fd07c993153d1f")
      .map(d =>
      {
        log.info(d+"")
        log.info(d.body)
      })

//    wechat.accessToken

    Future(Done)
  })

  override def post = ???
}
