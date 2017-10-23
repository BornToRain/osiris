package com.oasis.osiris.tool

import akka.Done
import com.lightbend.lagom.scaladsl.api.transport.{MessageProtocol, RequestHeader, ResponseHeader}
import play.api.http.HeaderNames

import scala.concurrent.{ExecutionContext, Future}

/**
	* Restful风格返回工具
	*/
object Restful
{
	type Response = (ResponseHeader, Done)

	/**
		* Http资源创建成功 => 状态码201
		*/
	def created(request: RequestHeader)(id: String)(implicit ec: ExecutionContext): Future[Response] = Future
	{
		(ResponseHeader(201,
			MessageProtocol(Some("application/json"), Some("UTF-8"), None), Vector.empty)
		.withHeader(HeaderNames.LOCATION, request.uri + "/" + id), Done)
	}

	/**
		* Http资源删除成功 => 状态码204
		*
		* @param ec
		* @return
		*/
	def noContent(implicit ec: ExecutionContext): Future[Response] = Future
	{
		(ResponseHeader(204, MessageProtocol.empty, Vector.empty), Done)
	}
}
