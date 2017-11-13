import sbt._

//包名
object Name
{
  val macwire    = "com.softwaremill.macwire"
  val cassandra  = "com.datastax.cassandra"
  val simulacrum = "com.github.mpilquist"
  val redis      = "com.github.etaty"
  val aliyunMns  = "com.aliyun.mns"
  val xstream    = "com.thoughtworks.xstream"
}

//版本
object Version
{
  val macwire    = "2.2.5"
  val cassandra  = "3.3.0"
  val simulacrum = "0.11.0"
  val redis      = "1.8.0"
  val aliyunMns  = "1.1.8"
  val xstream    = "1.4.10"
}

//jar仓库
object Library
{
  val macwire         = Name.macwire %% "macros" % Version.macwire % "provided"
  val cassandraExtras = Name.cassandra % "cassandra-driver-extras" % Version.cassandra
  val simulacrum      = Name.simulacrum %% "simulacrum" % Version.simulacrum
  val redis           = Name.redis %% "rediscala" % Version.redis
  val aliyunMns       = Name.aliyunMns % "aliyun-sdk-mns" % "1.1.8"
  val xstream         = Name.xstream % "xstream" % Version.xstream
}
