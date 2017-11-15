package com.oasis.osiris.wechat.impl

import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import com.oasis.osiris.tool.db.Implicits._
import com.oasis.osiris.tool.functional.Lift.ops._

import scala.concurrent.ExecutionContext


/**
  * 仓库
  */
class MenuRepository(session: CassandraSession)(implicit ec: ExecutionContext)
{
  import com.oasis.osiris.wechat.impl.client.WechatClient.{Button, ResetMenu}

  /**
    * 获取重置微信菜单数据
    */
  def getReset = for
  {
    xs      <- session.selectAll("SELECT * FROM menu")
    //全部菜单
    menus   <- xs.map
    {
      row =>
      val id = row.getString("id")
      val name = row.getString("name")
      val `type` = row.getImplicitly[Option[String]]("type").map(MenuType.withName)
      val key = row.getImplicitly[Option[String]]("key")
      val uri = row.getImplicitly[Option[String]]("uri")
      val parentId = row.getImplicitly[Option[String]]("parent_id")
      val sort = row.getInt("sort")
      val isShow = row.getBool("is_show")
      val createTime = row.getTimestamp("create_time").toInstant
      val updateTime = row.getTimestamp("update_time").toInstant

      Menu(id, name, `type`, key, uri, parentId, sort, isShow, createTime, updateTime)
    }.liftF
    //顶级菜单
    tops    <- menus.filter(_.parentId.isEmpty)
    //排序 数字越小越前面
    .sortBy(_.sort)
    .liftF
    //转成微信需要的请求格式
    buttons <- tops
    .map
    {
      d =>
      val sub = menus.filter(_.parentId match
      {
        case Some(s) => s == d.id
        case None    => false
      })
      //排序 数字越小越前面
      .sortBy(_.sort)
      .map(sub => Button(sub.`type`,sub.name,sub.key,sub.uri,Nil))

      Button(d.`type`, d.name, d.key, d.uri, sub)
    }
    .liftF
    //重置菜单数据
    result  <- ResetMenu(buttons,None).liftF
  } yield result
}

object Test extends App
{
  val s:Option[String] = Some("1")

  val s1 = "1"

  println(s.map(_ == s1))
}