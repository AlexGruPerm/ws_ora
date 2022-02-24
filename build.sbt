ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version      := "1.0.0"

lazy val root = (project in file("."))
  .settings(
    name := "wsora"
  )

lazy val Versions = new {
  val akka = "2.6.3"
  val akkaHttp  = "10.1.10"
  val circeVers = "0.12.3"
  val logbackVers = "1.2.3"
  val zioVers = "1.0.3"
  val zioConf = "1.0.0-RC29"
  val magnoliaVersion = "1.0.0-RC29"
  val zioConfTypeSafe = "1.0.0-RC29"
  val zioLog = "0.4.0"
  val zioLogSlf4j = "0.4.0"
  val dbcp2Vers = "2.8.0"
}

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.bintrayRepo("websudos", "oss-releases"),
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("public")
)

resolvers += Classpaths.typesafeReleases
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

//paradise for using circe annotations, f.e. @JsonCodec case class DbErrorDesc
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % Versions.logbackVers,
  "dev.zio" %% "zio" % Versions.zioVers,
  "dev.zio" % "zio-config-magnolia_2.12" % Versions.magnoliaVersion,
  "dev.zio" % "zio-config-typesafe_2.12" % Versions.zioConfTypeSafe,
  "dev.zio" %% "zio-config" % Versions.zioConf,
  "dev.zio" %% "zio-logging" % Versions.zioLog,
  "dev.zio" % "zio-logging-slf4j_2.12" % Versions.zioLogSlf4j,
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

assembly / assemblyMergeStrategy := {
  case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
  case PathList("module-info.class") => MergeStrategy.discard
  case x if x.endsWith("/module-info.class") => MergeStrategy.discard
  case PathList("META-INF", xs @ _*)         => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}

assembly / assemblyJarName := "wsora.jar"