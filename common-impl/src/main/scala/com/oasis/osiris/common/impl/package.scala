package com.oasis.osiris.common

import scala.concurrent.{ExecutionContext, Future}

package object impl
{
	/**
		* 升格成Future
		*/
	implicit class ObjectToFuture[T](data: T)
	{
		def liftF(implicit ec: ExecutionContext) = Future(data)
	}

}
