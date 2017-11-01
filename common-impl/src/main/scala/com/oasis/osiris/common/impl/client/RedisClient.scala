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
  lazy val host     = ConfigFactory.load.getString("redis.host")
  lazy val port     = ConfigFactory.load.getInt("redis.port")
  lazy val password = ConfigFactory.load.getString("redis.password")
}
