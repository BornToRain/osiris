package com.oasis.osiris.wechat.impl
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity

/**
  * 持久化
  */
//标签持久化
class TagEntity extends PersistentEntity
{
  import akka.Done
  import com.oasis.osiris.wechat.impl.TagCommand.{AddFans, Create}
  import com.oasis.osiris.wechat.impl.TagEvent.{AddedFans, Created}

  override type Command = TagCommand[_]
  override type Event = TagEvent
  override type State = Option[Tag]

  override def initialState = None

  override def behavior =
  {
    case None => nonexistence
    case _    => existence
  }

  //不存在状态下操作
  def nonexistence = Actions()
  //处理创建命令
  .onCommand[Create, String]
  {
    //持久化创建事件回复聚合根Id
    case ((cmd: Create), ctx, _) => ctx.thenPersist(Created(cmd))(e => ctx.reply(e.cmd.id))
  }
  .onEvent
  {
    //创建聚合根
    case (Created(cmd), _) => Some(Tag(cmd.id, cmd.wxId, cmd.name, 0L, Vector.empty, cmd.createTime, cmd.updateTime))
  }

  //已存在状态下操作
  def existence = Actions()
  //处理添加粉丝命令
  .onCommand[AddFans, Done]
  {
    //持久化添加粉丝命令回复完成
    case ((cmd: AddFans), ctx, _) => ctx.thenPersist(AddedFans(cmd))(_ => ctx.reply(Done))
  }
  .onEvent
  {
    //添加粉丝
    case (AddedFans(cmd), state) => state.map(_.addFans(cmd.openIds: _*))
  }
}

//二维码持久化
class QRCodeEntity extends PersistentEntity
{
  import com.oasis.osiris.wechat.impl.QRCodeCommand.Create
  import com.oasis.osiris.wechat.impl.QRCodeEvent.Created
  override type Command = QRCodeCommand[_]
  override type Event = QRCodeEvent
  override type State = Option[QRCode]

  override def initialState = None

  override def behavior =
  {
    case None => nonexistence
    case _    => throw new UnsupportedOperationException("不支持的操作")
  }

  //不存在状态下操作
  def nonexistence = Actions()
  //处理创建命令
  .onCommand[Create,String]
  {
    //持久化创建事件回复聚合根Id
    case ((cmd:Create),ctx, _) => ctx.thenPersist(Created(cmd))(e => ctx.reply(e.cmd.id))
  }
  .onEvent
  {
    //创建聚合根
    case (Created(cmd), _) => Some(QRCode(cmd.id,cmd.`type`,cmd.sceneStr,cmd.sceneId,cmd.expireSeconds,cmd.ticket,cmd.uri,cmd.createTime,cmd.updateTime))
  }
}
