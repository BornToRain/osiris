package com.oasis.osiris.common.impl.client

import akka.actor.ActorSystem
import redis.{RedisClient => Redis}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Redis客户端
  */
case class RedisClient(actorSystem: ActorSystem)(implicit ec:ExecutionContext)
{
  lazy val client = Future(Redis(RedisClient.host, RedisClient.port, Some(RedisClient.password))(actorSystem))
}

object RedisClient
{
  import com.typesafe.config.ConfigFactory
  lazy val host     = ConfigFactory.load.getString("redis.host")
  lazy val port     = ConfigFactory.load.getInt("redis.port")
  lazy val password = ConfigFactory.load.getString("redis.password")
}
