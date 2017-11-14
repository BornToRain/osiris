package com.oasis.osiris.wechat.impl
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry

/**
  * 序列化
  */
object SerializerRegistry extends JsonSerializerRegistry
{
  import com.lightbend.lagom.scaladsl.playjson.JsonSerializer
  def serializers = Vector(
    //命令
    //二维码
    JsonSerializer[QRCodeCommand.Create],
    //事件
    //二维码
    JsonSerializer[QRCodeEvent.Created],
    //聚合根
    //二维码
    JsonSerializer[QRCode]
  )
}
