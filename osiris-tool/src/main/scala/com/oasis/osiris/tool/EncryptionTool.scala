package com.oasis.osiris.tool

import java.security.MessageDigest
import java.util.Base64

/**
  * 加解密工具
  */
object EncryptionTool
{
  lazy val MD5    = "MD5"
  lazy val SHA1   = "SHA1"
  lazy val BASE64 = "Base64"
  lazy val HMAC   = "HmacSHA1"

  //MD5加密
  def md5(s: String): String = MessageDigest.getInstance(MD5)
  .digest(s.getBytes).map("%02x".format(_)).mkString

  //SHA1加密
  def SHA1(s: String): String = MessageDigest.getInstance(SHA1)
  .digest(s.getBytes).map("%02x".format(_)).mkString

  //Base64加解密 默认加密
  def base64(s: String)(implicit op: String = "en"): String = op match
  {
    //加密
    case "en" => new String(Base64.getEncoder.encode(s.getBytes))
    //解密
    case _ => new String(Base64.getDecoder.decode(s.getBytes))
  }

  //HMAC-SHA1加密
  def hmac(k: String)(v: String): String =
  {
    import javax.crypto.Mac
    import javax.crypto.spec.SecretKeySpec
    val secretKey = new SecretKeySpec(k.getBytes, HMAC)
    val mac = Mac.getInstance(HMAC)
    mac.init(secretKey)
    new String(Base64.getEncoder.encode(mac.doFinal(v.getBytes)))
  }
}
