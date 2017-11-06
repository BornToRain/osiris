package com.oasis.osiris.tool

import akka.actor.ActorSystem
import redis.RedisClient

/**
  * Redis客户端
  */
class RedisTool(actorSystem: ActorSystem)
{
  lazy val client = RedisClient(RedisTool.host, RedisTool.port, Some(RedisTool.password))(actorSystem)
}

object RedisTool
{
  import com.typesafe.config.ConfigFactory

  private[this] val config   = ConfigFactory.load
  private[tool] val host     = config.getString("redis.host")
  private[tool] val port     = config.getInt("redis.port")
  private[tool] val password = config.getString("redis.password")
}

