import sbt._

//包名
object Name
{
	val macwire    = "com.softwaremill.macwire"
	val cassandra  = "com.datastax.cassandra"
	val simulacrum = "com.github.mpilquist"
}

//版本
object Version
{
	val macwire    = "2.2.5"
	val cassandra  = "3.3.0"
	val simulacrum = "0.11.0"
}

//jar仓库
object Library
{
	val macwire         = Name.macwire %% "macros" % Version.macwire % "provided"
	val cassandraExtras = Name.cassandra % "cassandra-driver-extras" % Version.cassandra
	val simulacrum      = Name.simulacrum %% "simulacrum" % Version.simulacrum
}
