package com.oasis.osiris.wechat.impl
import akka.actor.ActorSystem
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.oasis.osiris.wechat.api.WechatService

import scala.concurrent.ExecutionContext

/**
  * 微信接口实现
  */
class WechatServiceImpl
(
  registry:PersistentEntityRegistry,
  actorSystem: ActorSystem
)(implicit ec:ExecutionContext)extends WechatService
{
  override def get(signature: String, timestamp: String, nonce: String,
    echostr                 : String) = ???

  override def post = ???
}
