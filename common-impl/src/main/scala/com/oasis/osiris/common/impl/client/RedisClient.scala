package com.oasis.osiris.common.impl.client

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
  private[this]         val config   = ConfigFactory.load
  private[RedisClient$] val host     = config.getString("redis.host")
  private[RedisClient$] val port     = config.getInt("redis.port")
  private[RedisClient$] val password = config.getString("redis.password")
}
