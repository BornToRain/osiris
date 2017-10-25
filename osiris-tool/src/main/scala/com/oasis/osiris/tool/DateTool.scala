package com.oasis.osiris.tool

import java.time.Instant

/**
	* 日期工具包
	*/
object DateTool
{
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
}
