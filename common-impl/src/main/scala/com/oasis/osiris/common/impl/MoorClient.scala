package com.oasis.osiris.common.impl

import play.api.libs.ws.WSClient

sealed trait MoorRequest[T]

object MoorRequest
{

	//挂断请求
	case class HangUp(CallId: String, Agent: String, ActionID: String) extends MoorRequest[String]

//	implicit def liftF[T](fa: MoorRequest[T]): Future[MoorRequest[T]] = Future(fa)
}

class MoorClient(baseUri: String)(ws: WSClient)(config: MoorConfiguration)
{
	def hangUp = ws.url(baseUri + "v20160818/call/hangup/").get

//	def test = for
//		{
//			a <- HangUp("","","")
//		}yield a
}

class MoorConfiguration()

//sealed trait Calc[+A]
//2 object Calc {
//	3   case class Push(value: Int) extends Calc[Unit]
//	4   case class Add() extends Calc[Unit]
//	5   case class Mul() extends Calc[Unit]
//	6   case class Div() extends Calc[Unit]
//	7   case class Sub() extends Calc[Unit]
//	8   implicit def calcToFree[A](ca: Calc[A]) = Free.liftFC(ca)
//	9 }
//10 import Calc._
//11 val ast = for {
//12   _ <- Push(23)
//13   _ <- Push(3)
//14   _ <- Add()
//15   _ <- Push(5)
//16   _ <- Mul()
//17 } yield ()

//class MoorClient(ws: WSClient, uri: String)
//{
//	@Inject
//	def this(ws: WSClient) = this(ws, "http://apis.7moor.com/")
//
//	def hangUp(request: HangUp) =
//	{
//		ws.url(uri + "v20160818/call/hangup/")
//	}
//}
//
//class MoorConfigure
//{
//
//}
