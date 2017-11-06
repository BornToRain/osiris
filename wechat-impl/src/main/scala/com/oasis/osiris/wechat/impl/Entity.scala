package com.oasis.osiris.wechat.impl
import akka.actor.ActorSystem
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity

/**
  * 持久化
  */
class WechatEntity(actorSystem: ActorSystem) extends PersistentEntity
{
  override type Command = this.type
  override type Event = this.type
  override type State = this.type

  override def initialState = ???

  override def behavior = ???
}