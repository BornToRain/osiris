package com.oasis.osiris.wechat

package object impl
{
  /**
    * 响应微信公众号
    */
  lazy val success = "success"

  /**
    * 领域模型与接口对象转换器
    */
  implicit class DomainModelConverter(data: AnyRef)
  {
    /**
      * 转接口对象
      */
    def toApi[T]: T =
    {
      val obj = data match
      {
        case d: Enumeration#Value => d match
        {
          case QRCodeType.QR_SCENE           => api.QRCodeDTO.QRCodeType.QR_SCENE
          case QRCodeType.QR_STR_SCENE       => api.QRCodeDTO.QRCodeType.QR_STR_SCENE
          case QRCodeType.QR_LIMIT_SCENE     => api.QRCodeDTO.QRCodeType.QR_LIMIT_SCENE
          case QRCodeType.QR_LIMIT_STR_SCENE => api.QRCodeDTO.QRCodeType.QR_LIMIT_STR_SCENE
        }
      }
      obj.asInstanceOf[T]
    }

    /**
      * 转领域模型
      */
    def toDomain[T]: T =
    {
      val obj = data match
      {
        case d: Enumeration#Value => d match
        {
          case api.QRCodeDTO.QRCodeType.QR_SCENE           => QRCodeType.QR_SCENE
          case api.QRCodeDTO.QRCodeType.QR_STR_SCENE       => QRCodeType.QR_STR_SCENE
          case api.QRCodeDTO.QRCodeType.QR_LIMIT_SCENE     => QRCodeType.QR_LIMIT_SCENE
          case api.QRCodeDTO.QRCodeType.QR_LIMIT_STR_SCENE => QRCodeType.QR_LIMIT_STR_SCENE
        }
      }
      obj.asInstanceOf[T]
    }
  }
}
