name := "osiris"
version in ThisBuild := "2.0"
scalaVersion in ThisBuild := "2.11.11"
organization in ThisBuild := "com.oasis.osiris"

//模块基础配置
def project(id: String) = Project(id, base = file(id))
.settings(
  javacOptions in compile ++= Seq(
    "-encoding", "UTF-8",
    "-source", "1.8",
    "-target", "1.8",
    "-Xlint:unchecked",
    "-Xlint:deprecation"
  ),
  scalacOptions in compile ++= Seq(
    "-Yartart-unification"
  ),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
)

//Cassandra每次重启时候清空数据
lagomCassandraCleanOnStart in ThisBuild := true

//微服务工具包、配置模块
lazy val `osiris-tool` = project("osiris-tool")
.settings(
  name := "osiris-tool",
  libraryDependencies ++= Seq(
    Library.redis,
    Library.simulacrum,
    Library.cassandraExtras,
    lagomScaladslApi % Optional,
    lagomScaladslServer % Optional
  )
)
//公用模块接口
lazy val `common-api` = project("common-api")
.settings(
  name := "common-api",
  libraryDependencies ++= Seq(
    lagomScaladslApi
  )
)
.dependsOn(`osiris-tool`)
//公用模块实现
lazy val `common-impl` = project("common-impl")
.enablePlugins(LagomScala)
.settings(
  name := "common-impl",
  libraryDependencies ++= Seq(
    Library.redis,
    Library.macwire,
    Library.aliyunMns,
    lagomScaladslServer,
    lagomScaladslPersistenceCassandra
  )
)
.dependsOn(`osiris-tool`, `common-api`)
