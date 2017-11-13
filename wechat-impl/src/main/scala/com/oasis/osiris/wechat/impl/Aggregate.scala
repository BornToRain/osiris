package com.oasis.osiris.wechat.impl
import java.time.Instant

/**
  * 领域聚合根
  */
//标签聚合根
case class Tag
(
  id: String,
  //微信标签ID
  wxId: String,
  name: String,
  //粉丝数
  fans: Long,
  //粉丝列表
  openIds: Vector[String],
  createTime: Instant,
  updateTime: Instant
)
{
  def addFans(openId: String*):Tag = copy(openIds = openIds ++ openId)
}
