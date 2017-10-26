package com.oasis.osiris.tool

import java.time.Instant

import org.joda.time.{DateTime, Duration}

/**
	* 日期工具包
	*/
object DateTool
{
	lazy val SECONDS   = "seconds"
	lazy val MINUTES   = "minutes"
	lazy val HOURS     = "hours"
	lazy val DAYS      = "days"
	lazy val FULLDATE  = "yyyy-MM-dd HH:mm:ss"
	lazy val TIMESTAMP = "yyyyMMddHHmmss"

	//字符串转Instant格式日期
	def toInstant(str: String) = str match
	{
		case s if s.contains("T") && s.contains("Z") => Instant.parse(s)
		case s                                       =>
			//需要给时间添加时区才能正确解析
		val utc = s.collect
		{
			case ' ' => 'T'
			case c   => c
		} + 'Z'
		Instant.parse(utc)
	}

	//x与y的时间差
	//x<y?正数:负数 默认分钟
	def compareTO(x: Instant)(y: Instant)(`type`: String = MINUTES) =
	{
		val d = new Duration(new DateTime(x), new DateTime(y))
		`type` match
		{
			case SECONDS => d.getStandardSeconds
			case MINUTES => d.getStandardMinutes
			case HOURS   => d.getStandardHours
			case DAYS    => d.getStandardDays
			case _       => d.getMillis
		}
	}

	//yyyyMMddHHmmss格式时间戳
	def datetimeStamp = DateTime.now.toString(TIMESTAMP)
}
