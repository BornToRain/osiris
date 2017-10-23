import sbt._

//包名
object Name
{
	val typeLevel = "org.typelevel"
	val macwire   = "com.softwaremill.macwire"
	val cassandra = "com.datastax.cassandra"
}

//版本
object Version
{
	val cats    = "1.0.0-MF"
	val macwire = "2.2.5"
	val cassandra = "3.3.0"
}

//jar仓库
object Library
{
	val catsCore = Name.typeLevel %% "cats-core" % Version.cats
	val catsFree = Name.typeLevel %% "cats-free" % Version.cats
	val macwire  = Name.macwire %% "macros" % Version.macwire % "provided"
	val cassandraCore = Name.cassandra % "cassandra-driver-core" % Version.cassandra
	val cassandraExtras = Name.cassandra % "cassandra-driver-extras" % Version.cassandra
}
