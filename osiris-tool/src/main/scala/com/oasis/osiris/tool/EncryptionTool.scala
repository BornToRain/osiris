package com.oasis.osiris.tool

import java.security.MessageDigest
import java.util.Base64

/**
  * 加解密工具
  */
object EncryptionTool
{
  //MD5加密
  def md5(s: String) = MessageDigest.getInstance("MD5")
  .digest(s.getBytes).map("%02x".format(_)).mkString

  //Base64加解密 默认加密
  def base64(s: String)(implicit op: String = "en") = op match
  {
    //加密
    case "en" => new String(Base64.getEncoder.encode(s.getBytes))
    //解密
    case _ => new String(Base64.getDecoder.decode(s.getBytes))
  }
}
