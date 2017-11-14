package com.oasis.osiris.wechat.impl
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomApplicationLoader}
import play.api.libs.ws.ahc.AhcWSComponents

/**
  * 微信模块启动
  */
class WechatAppLoader extends LagomApplicationLoader
{
  import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
  import com.lightbend.lagom.scaladsl.server.LagomApplicationContext
  import com.oasis.osiris.wechat.api.WechatService
  import com.typesafe.conductr.bundlelib.lagom.scaladsl.ConductRApplicationComponents

  //正式环境启动
  override def load(context: LagomApplicationContext) = new WechatApp(context) with ConductRApplicationComponents

  //测试环境启动
  override def loadDevMode(context: LagomApplicationContext) = new WechatApp(context) with LagomDevModeComponents

  //服务描述
  override def describeService = Some(readDescriptor[WechatService])
}

abstract class WechatApp(context: LagomApplicationContext) extends LagomApplication(context)
//Cassandra插件
with CassandraPersistenceComponents
//WS-Http插件
with AhcWSComponents
{
  import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
  import com.lightbend.lagom.scaladsl.server.LagomServer
  import com.oasis.osiris.tool.RedisTool
  import com.oasis.osiris.wechat.api.WechatService
  import com.oasis.osiris.wechat.impl.client.WechatClient
  import com.softwaremill.macwire._
  import redis.RedisClient

  //Redis客户端
  lazy val redisClient           : RedisClient            = wire[RedisTool].client
  //微信公众号客户端
  lazy val wechatClient          : WechatClient           = wire[WechatClient]
  //绑定服务
  lazy val lagomServer           : LagomServer            = serverFor[WechatService](wire[WechatServiceImpl])
  //注册序列化
  lazy val jsonSerializerRegistry: JsonSerializerRegistry = SerializerRegistry
  //持久化
  persistentEntityRegistry.register(wire[QRCodeEntity])
  //事件处理
  readSide.register(wire[QRCodeEventProcessor])
  //启动微信公众号
  wechatClient.startWechat
}
