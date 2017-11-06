package com.oasis.osiris.wechat.impl
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry

/**
  * 序列化
  */
object SerializerRegistry extends JsonSerializerRegistry
{
  def serializers = Vector.empty
}
