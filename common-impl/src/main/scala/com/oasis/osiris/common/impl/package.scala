package com.oasis.osiris.common

package object impl
{
  //手机号绑定关系在Redis的Key
  lazy val REDIS_KEY_BINDING = "binding=>"

  /**
    * 领域模型与接口对象转换器
    */
  implicit class DomainModelConverter[K, V](data: AnyRef)
  {
    //转接口对象
    def toApi[T]: T =
    {
      val obj = data match
      {
        case d: Enumeration#Value => d match
        {
          case SmsType.login      => api.SmsDTO.SmsType.login
          case SmsType.invitation => api.SmsDTO.SmsType.invitation
          case SmsType.register   => api.SmsDTO.SmsType.register
          case SmsType.payment    => api.SmsDTO.SmsType.payment
          case SmsType.notice     => api.SmsDTO.SmsType.notice
        }
      }
      obj.asInstanceOf[T]
    }

    //转领域模型
    def toDomain[T]: T =
    {
      val obj = data match
      {
        case d: Enumeration#Value => d match
        {
          case api.SmsDTO.SmsType.login      => SmsType.login
          case api.SmsDTO.SmsType.invitation => SmsType.invitation
          case api.SmsDTO.SmsType.register   => SmsType.register
          case api.SmsDTO.SmsType.payment    => SmsType.payment
          case api.SmsDTO.SmsType.notice     => SmsType.notice
        }
      }
      obj.asInstanceOf[T]
    }
  }
}
