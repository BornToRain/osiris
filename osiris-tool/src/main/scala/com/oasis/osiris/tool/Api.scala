package com.oasis.osiris.tool

import akka.event.slf4j.SLF4JLogging
import com.lightbend.lagom.scaladsl.api.transport.{ExceptionMessage, Method, TransportErrorCode, UnsupportedMediaType}
import com.lightbend.lagom.scaladsl.server.ServerServiceCall
import play.api.http.HeaderNames

/**
  * 接口日志、权限等
  */
trait Api extends SLF4JLogging
{
  import play.api.http.ContentTypes
  lazy val v2Json = "application/oasis.v2+json"
  //v2版本头
  def v2[Request, Response](call: ServerServiceCall[Request, Response]) = logged(ServerServiceCall.compose
  {
    request =>
    request.method match
    {
      //Get请求默认获取最新版本数据
      case Method.GET => call
      case _          => request.getHeader(HeaderNames.ACCEPT) match
      {
        case Some(d) if d == ContentTypes.JSON || d == v2Json => call
        case _                                                => throw new UnsupportedMediaType(TransportErrorCode.ProtocolError,
          new ExceptionMessage("ProtocolError", s"请指定Accept请求头为${ContentTypes.JSON }或${v2Json }"))
      }
    }
  })
  //日志
  def logged[Request, Response](call: ServerServiceCall[Request, Response]) = ServerServiceCall.compose
  {
    request =>
    log.info(s"请求方式 =====> ${request.method }")
    log.info(s"请求URI  =====> ${request.uri }")
    if (log.isDebugEnabled)
    {
      log.debug(s"请求内容协议 =====> ${request.protocol.contentType }")
      log.debug(s"请求内容编码 =====> ${request.protocol.charset }")
      request.headerMap.foreach
      {
        case (_, Seq((k, v))) => log.debug(s"$k =====> $v")
      }
    }
    call
  }
}


