package com.oasis.osiris.wechat.impl
import akka.Done
import akka.event.slf4j.SLF4JLogging
import java.util.Date
import com.datastax.driver.core.PreparedStatement
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, ReadSideProcessor}
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.oasis.osiris.tool.db.Implicits._

import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * 领域事件处理
  */
//二维码领域事件处理
class QRCodeEventProcessor(session : CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext)
extends ReadSideProcessor[QRCodeEvent] with SLF4JLogging
{
  import java.time.Instant
  import java.util.Date

  import com.datastax.driver.core.PreparedStatement
  import com.lightbend.lagom.scaladsl.persistence.EventStreamElement
  import com.oasis.osiris.tool.db.{IntCodec, OptionCodec}
  import com.oasis.osiris.wechat.impl.QRCodeEvent.Created

  import scala.concurrent.Promise

  private[this] val insertPro = Promise[PreparedStatement]

  //当前集群
  private[this] def getCluster = session.underlying.map(_.getCluster)

  //编码器列表
  private[this] val codecs = Seq(OptionCodec(IntCodec), OptionCodec[String], OptionCodec[Instant])

  override def buildHandler = readSide.builder[QRCodeEvent]("qrcodeEventOffSet")
  .setGlobalPrepare(() => createTable)
  .setPrepare(_ => prepare)
  .setEventHandler[Created](created)
  .build

  override def aggregateTags = QRCodeEvent.tag.allTags

  private[this] def createTable = for
  {
    //二维码表
    _ <- session.executeCreateTable
    {
      """
        |CREATE TABLE IF NOT EXISTS qr_code
        |(
        | id text PRIMARY KEY,
        | type text,
        | scene_str text,
        | scene_id int,
        | expire_seconds int,
        | ticket text,
        | uri text,
        | create_time timestamp,
        | update_time timestamp
        |)
      """.stripMargin
    }
  } yield Done

  private[this] def prepare =
  {
    //设置集群编码器
    getCluster.map(_.getConfiguration.getCodecRegistry.register(codecs: _*))
    //二维码插入
    insertPro.completeWith(session.prepare(
      """
        |INSERT INTO qr_code(id,type,scene_str,scene_id,expire_seconds,ticket,uri,create_time,update_time)
        |VALUES (?,?,?,?,?,?,?,?,?)
      """.stripMargin))
    Future(Done)
  }

  //创建二维码
  private[this] def created(event: EventStreamElement[Created]) =
  {
    log.info("持久化二维码到读边")
    val cmd = event.event.cmd
    for
    {
      data <- insertPro.future.map
      {
        ps =>
        val d = ps.bind
        d.setString("id", cmd.id)
        d.setString("type", cmd.`type`.toString)
        d.setImplicitly("scene_str", cmd.sceneStr)
        d.setImplicitly("scene_id", cmd.sceneId)
        d.setImplicitly("expire_seconds", cmd.expireSeconds)
        d.setString("ticket", cmd.ticket)
        d.setString("uri", cmd.uri)
        d.setTimestamp("create_time", Date.from(cmd.createTime))
        d.setTimestamp("update_time", Date.from(cmd.updateTime))
      }
    } yield Vector(data)
  }
}

//菜单事件处理
class MenuEventProcessor(session: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext)
extends ReadSideProcessor[MenuEvent] with SLF4JLogging
{
  import com.oasis.osiris.wechat.impl.MenuEvent.Created

  private[this] val insertPro = Promise[PreparedStatement]

  override def buildHandler = readSide.builder[MenuEvent]("menuEventOffSet")
  .setGlobalPrepare(() => createTable)
  .setPrepare(_ => prepare)
  .setEventHandler[Created](created)
  .build

  override def aggregateTags = MenuEvent.tag.allTags

  private[this] def createTable = for
  {
    //菜单表
    _ <- session.executeCreateTable
    {
      """
        |CREATE TABLE IF NOT EXISTS menu
        |(
        | id text PRIMARY KEY,
        | type text,
        | name text,
        | key text,
        | uri text,
        | parent_id text,
        | sort int,
        | is_show bool,
        | create_time timestamp,
        | update_time timestamp
        |)
      """.stripMargin
    }
  } yield Done

  private[this] def prepare =
  {
    //菜单插入
    insertPro.completeWith(session.prepare(
      """
        |INSERT INTO menu(id,type,name,key,uri,parent_id,sort,is_show,create_time,update_time)
        |VALUES (?,?,?,?,?,?,?,?,?,?)
      """.stripMargin))
    Future(Done)
  }

  //创建二维码
  private[this] def created(event: EventStreamElement[Created]) =
  {
    log.info("持久化菜单到读边")
    val cmd = event.event.cmd
    for
    {
      data <- insertPro.future.map
      {
        ps =>
        val d = ps.bind
        d.setString("id", cmd.id)
        d.setString("type", cmd.`type`.toString)
        d.setString("name", cmd.name)
        d.setImplicitly("key", cmd.key)
        d.setImplicitly("uri", cmd.uri)
        d.setImplicitly("parent_id", cmd.parentId)
        d.setInt("sort", cmd.sort)
        d.setBool("is_show", cmd.isShow)
        d.setTimestamp("create_time", Date.from(cmd.createTime))
        d.setTimestamp("update_time", Date.from(cmd.updateTime))
      }
    } yield Vector(data)
  }
}