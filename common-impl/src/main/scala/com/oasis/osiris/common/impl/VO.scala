package com.oasis.osiris.common.impl

/**
  * 领域值对象
  */
//绑定关系
case class BindingRelation
(
  callUpRecordId:String,
  call:String,
  called:String,
  thirdId:Option[String]
)

object BindingRelation
{
  import play.api.libs.json.{Format, Json}
  import redis.ByteStringFormatter

  implicit val format:Format[BindingRelation] = Json.format

  implicit val redisFormat = new ByteStringFormatter[BindingRelation]
  {
    import akka.util.ByteString
    override def deserialize(bs: ByteString) = Json.parse(bs.utf8String).as[BindingRelation]

    override def serialize(data: BindingRelation) = ByteString(s"${Json.toJson(data)}")
  }
}
