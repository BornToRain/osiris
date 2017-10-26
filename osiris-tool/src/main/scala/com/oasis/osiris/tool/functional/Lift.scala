package com.oasis.osiris.tool.functional

import simulacrum._

import scala.concurrent.{ExecutionContext, Future}

/**
	* 升格成FutureMonad
	*
	* @tparam T
	*/
@typeclass trait Lift[T]
{
	def liftF(d: T): Future[T]
}

object Lift
{
	/**
		* 任意类型升格
		*/
	implicit def t2F[T](implicit ec:ExecutionContext) = new Lift[T]
	{
		override def liftF(d: T) = Future(d)
	}
}