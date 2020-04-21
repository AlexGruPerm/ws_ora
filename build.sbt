name := "WsOra"

version := "1.0"

scalaVersion := "2.12.8"

lazy val Versions = new {
  val akka = "2.6.3"
  val akkaHttp  = "10.1.10"
  val circeVers = "0.12.3"
  val logbackVers = "1.2.3"
  //val pgVers = "42.2.5"
  val zioVers = "1.0.0-RC18-2"
  val zioConf = "1.0.0-RC16-2"
  val magnoliaVersion = "1.0.0-RC16-2"
  val zioConfTypeSafe =  "1.0.0-RC16"//"1.0.0-RC16-2"
  val zioLog = "0.2.5"
  val zioLogSlf4j = "0.2.5"
  val dbcp2Vers = "2.7.0"
  val jschVers = "0.1.55"
  //val typeSefeConf = "1.4.0"
}

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.bintrayRepo("websudos", "oss-releases"),
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("public")
  //Resolver.typesafeRepo("releases"),
)

resolvers += Classpaths.typesafeReleases
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

//paradise for using circe annotations, f.e. @JsonCodec case class DbErrorDesc
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % Versions.logbackVers,
 // "com.typesafe" % "config" % Versions.typeSefeConf,
 // "org.postgresql" % "postgresql" % Versions.pgVers,
  "dev.zio" %% "zio" % Versions.zioVers,
  "dev.zio" % "zio-config-magnolia_2.12" % Versions.magnoliaVersion,
  "dev.zio" % "zio-config-typesafe_2.12" % "1.0.0-RC16",
 // "dev.zio" %% "zio-config-typesafe" % "1.0.0-RC16-2",
  "dev.zio" %% "zio-config-refined" % Versions.zioConf,
  "dev.zio" %% "zio-config" % Versions.zioConf,
  "dev.zio" %% "zio-logging" % Versions.zioLog,
  "dev.zio" %% "zio-logging-slf4j" % Versions.zioLogSlf4j,
  "org.apache.commons" % "commons-dbcp2" % Versions.dbcp2Vers
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % Versions.akka,
  "com.typesafe.akka" %% "akka-stream" % Versions.akka,
  "com.typesafe.akka" %% "akka-http" % Versions.akkaHttp,
  "com.typesafe.akka" %% "akka-actor-typed" % Versions.akka,
  "com.typesafe.akka" %% "akka-slf4j" % Versions.akka
)

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-literal"
).map(_ % Versions.circeVers)

assemblyMergeStrategy in assembly := {
  case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

assemblyJarName in assembly :="wsora.jar"
mainClass in (Compile, packageBin) := Some("application.Main")
mainClass in (Compile, run) := Some("application.Main")