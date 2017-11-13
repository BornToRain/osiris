package com.oasis.osiris.tool.serialization

/**
  * 自定义序列化
  */
object CustomMessageSerializer
{
  import com.lightbend.lagom.scaladsl.api.deser.StrictMessageSerializer

  import scala.xml.Elem

  /**
    * XML序列化
    */
  implicit val XMLMessageSerializer: StrictMessageSerializer[Elem] = new StrictMessageSerializer[Elem]
  {
    import akka.util.ByteString
    import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer
    import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer.{NegotiatedDeserializer, NegotiatedSerializer}
    import com.lightbend.lagom.scaladsl.api.transport.MessageProtocol

    import scala.collection.immutable
    import scala.xml.Elem

    private val defaultProtocol = MessageProtocol(Some("text/xml"), None, None)

    override def acceptResponseProtocols: immutable.Seq[MessageProtocol] = defaultProtocol :: MessageProtocol(Some("application/xml"), Some("UTF-8"),
      None) :: Nil

    private class XMLSerializer(override val protocol: MessageProtocol) extends NegotiatedSerializer[Elem, ByteString]
    {
      override def serialize(message: Elem): ByteString = ByteString.fromString(message.toString, protocol.charset.getOrElse("UTF-8"))
    }

    private class XMLDeserializer(charset: String) extends NegotiatedDeserializer[Elem, ByteString]
    {
      import scala.xml.XML
      override def deserialize(wire: ByteString): Elem = XML.loadString(wire.decodeString(charset))
    }

    override def deserializer(protocol: MessageProtocol): MessageSerializer.NegotiatedDeserializer[Elem, ByteString] = new XMLDeserializer("UTF-8")

    override def serializerForResponse(accepts: immutable.Seq[MessageProtocol]): NegotiatedSerializer[Elem, ByteString] = new XMLSerializer(
      accepts.find(_.contentType.contains("text/xml")).getOrElse(defaultProtocol))

    override def serializerForRequest: MessageSerializer.NegotiatedSerializer[Elem, ByteString] = new XMLSerializer(defaultProtocol)
  }
}

