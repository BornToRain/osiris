package com.oasis.osiris.common.impl
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server.{LagomApplication, LagomApplicationContext, LagomApplicationLoader}
import play.api.libs.ws.ahc.AhcWSComponents

/**
  * 公用模块启动
  */
class CommonAppLoader extends LagomApplicationLoader
{
  import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
  import com.lightbend.lagom.scaladsl.server.LagomApplicationContext
  import com.oasis.osiris.common.api.CommonService
  import com.typesafe.conductr.bundlelib.lagom.scaladsl.ConductRApplicationComponents
  //正式环境启动
  override def load(context: LagomApplicationContext) = new CommonApp(context) with ConductRApplicationComponents

  //测试环境启动
  override def loadDevMode(context: LagomApplicationContext) = new CommonApp(context) with LagomDevModeComponents

  //服务描述
  override def describeService = Some(readDescriptor[CommonService])
}

abstract class CommonApp(context: LagomApplicationContext) extends LagomApplication(context)
//Cassandra插件
with CassandraPersistenceComponents
//WS-Http插件
with AhcWSComponents
{

  import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
  import com.lightbend.lagom.scaladsl.server.LagomServer
  import com.oasis.osiris.common.api.CommonService
  import com.oasis.osiris.common.impl.client.{MoorClient, SmsClient}
  import com.oasis.osiris.tool.RedisTool
  import com.softwaremill.macwire._

  //Redis客户端
  lazy val redisClient           : redis.RedisClient      = wire[RedisTool].client
  //阿里云短信客户端
  lazy val smsClient             : SmsClient              = wire[SmsClient]
  //容联七陌客户端
  lazy val moorClient            : MoorClient             = wire[MoorClient]
  //绑定服务
  lazy val lagomServer           : LagomServer            = serverFor[CommonService](wire[CommonServiceImpl])
  //序列化
  lazy val jsonSerializerRegistry: JsonSerializerRegistry = SerializerRegistry
  //仓库
  lazy val callUpRecordRepository: CallUpRecordRepository = wire[CallUpRecordRepository]
  //持久化
  persistentEntityRegistry.register(wire[CallUpRecordEntity])
  persistentEntityRegistry.register(wire[SmsRecordEntity])
  //事件处理
  readSide.register(wire[CallUpRecordEventProcessor])
  readSide.register(wire[SmsRecordEventProcessor])
}

