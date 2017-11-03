package com.oasis.osiris.wechat.impl.client

import akka.actor.ActorSystem
import redis.{RedisClient => Redis}

/**
  * Redis客户端
  */
case class RedisClient(actorSystem: ActorSystem)
{
  lazy val client = Redis(RedisClient.host, RedisClient.port, Some(RedisClient.password))(actorSystem)
}

object RedisClient
{
  import com.typesafe.config.ConfigFactory
  private[this] lazy         val config   = ConfigFactory.load
  private[RedisClient$] lazy val host     = config.getString("redis.host")
  private[RedisClient$] lazy val port     = config.getInt("redis.port")
  private[RedisClient$] lazy val password = config.getString("redis.password")
}

