package com.oasis.osiris.tool

import akka.Done
import com.lightbend.lagom.scaladsl.api.transport.{MessageProtocol, RequestHeader, ResponseHeader}
import play.api.http.HeaderNames

/**
  * Restful风格返回工具
  */
object Restful
{
  type Response = (ResponseHeader, Done)
  /**
    * Http资源访问成功 => 状态码200
    */
  def ok: Response = (ResponseHeader(200, MessageProtocol(Some("application/json"), Some("UTF-8"), None), Vector.empty), Done)
  /**
    * Http资源创建成功 => 状态码201
    */
  def created(request: RequestHeader)(id: String): Response = (ResponseHeader(201,
    MessageProtocol(Some("application/json"), Some("UTF-8"), None), Vector.empty)
  .withHeader(HeaderNames.LOCATION, request.uri + "/" + id), Done)
  /**
    * Http资源删除成功 => 状态码204
    */
  def noContent: Response = (ResponseHeader(204, MessageProtocol.empty, Vector.empty), Done)
}
