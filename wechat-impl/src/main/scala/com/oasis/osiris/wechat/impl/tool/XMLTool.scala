package com.oasis.osiris.wechat.impl.tool
import com.thoughtworks.xstream.converters.Converter

object XMLTool
{
  import com.thoughtworks.xstream.XStream
  import com.thoughtworks.xstream.io.xml.XppDriver
  lazy val xstream = new XStream(new XppDriver()
  {
    import java.io.Writer

    import com.thoughtworks.xstream.io.HierarchicalStreamWriter
    import com.thoughtworks.xstream.io.xml.PrettyPrintWriter
    override def createWriter(out: Writer): HierarchicalStreamWriter = new PrettyPrintWriter(out)
    {
      import com.thoughtworks.xstream.core.util.QuickWriter
      //对所有XML节点转换都增加CDATA标记
      val cdata = true

      /**
        * 双下划线问题
        */
      override def encodeNode(name: String): String = name

      override def writeText(writer: QuickWriter, text: String) = if (cdata) writer.write(s"<![CDATA[$text]]>")
      else writer.write(text)
    }
  })
  xstream.autodetectAnnotations(true)
  xstream.registerConverter(new OptionConverter)
  xstream.registerConverter(new ScalaSeqConverter(xstream.getMapper))
  xstream.aliasSystemAttribute(null,"class")

  def toXML(data: AnyRef) =
  {
    import com.oasis.osiris.wechat.impl.WechatResponse.NewsArticle
    xstream.alias("xml", data.getClass)
    xstream.alias("item", classOf[NewsArticle])
    xstream.toXML(data)
  }
}

import com.thoughtworks.xstream.converters.{MarshallingContext, UnmarshallingContext}
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter
import com.thoughtworks.xstream.io.{HierarchicalStreamReader, HierarchicalStreamWriter}
import com.thoughtworks.xstream.mapper.Mapper

/**
  * Option解析
  */
class OptionConverter extends Converter
{
  override def marshal(source: scala.Any, writer: HierarchicalStreamWriter, context: MarshallingContext) =
  {
    val opt = source.asInstanceOf[Option[_]]
    for (value <- opt) context.convertAnother(value)
  }

  override def unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext) = Option(reader.getValue)

  override def canConvert(clazz: Class[_]) = classOf[Some[_]].isAssignableFrom(clazz) || clazz.isAssignableFrom(None.getClass)
}

/**
  * ScalaSeq解析
  */
class ScalaSeqConverter(_mapper  : Mapper) extends AbstractCollectionConverter(_mapper)
{
  def getAnyClass(x: Any) = x.asInstanceOf[AnyRef].getClass

  def canConvert(clazz: Class[_]) = classOf[::[_]] == clazz

  def marshal(value: Any, writer: HierarchicalStreamWriter, context: MarshallingContext) =
  {
    val xs = value.asInstanceOf[List[_]]
    for (item <- xs)
    {
      writeItem(item, context, writer)
    }
  }

  def unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext) =
  {
    var xs : List[_] = Nil
    while (reader.hasMoreChildren)
    {
      reader.moveDown
      val item = readItem(reader, context, xs)
      xs = xs ++ List(item)
      reader.moveUp
    }
    xs
  }
}

