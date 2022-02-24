ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version      := "1.0.0"

lazy val root = (project in file("."))
  .settings(
    name := "wsora",
    assembly / assemblyJarName := "wsora.jar",
    commonSettings,
    libraryDependencies ++= commonDependencies
  )

lazy val dependencies =
  new {
    val akkaVers = "2.6.3"
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

    val logback           =  "ch.qos.logback" % "logback-classic" % logbackVers
    val zio_main          =  "dev.zio" %% "zio" % zioVers
    val zio_conf_magnolia =  "dev.zio" % "zio-config-magnolia_2.12" % magnoliaVersion
    val zio_conf_typesafe =  "dev.zio" % "zio-config-typesafe_2.12" % zioConfTypeSafe
    val zio_conf          =  "dev.zio" %% "zio-config" % zioConf
    val zio_logg          =  "dev.zio" %% "zio-logging" % zioLog
    val zio_logg_slf4j    =  "dev.zio" % "zio-logging-slf4j_2.12" % zioLogSlf4j
    val apache_dbcp2      =  "org.apache.commons" % "commons-dbcp2" % dbcp2Vers
    val akka_actor        =  "com.typesafe.akka" %% "akka-actor" % akkaVers
    val akka_stream       =  "com.typesafe.akka" %% "akka-stream" % akkaVers
    val akka_http         =  "com.typesafe.akka" %% "akka-http" % akkaHttp
    val akka_actor_typed  =  "com.typesafe.akka" %% "akka-actor-typed" % akkaVers
    val akka_slf4j        =  "com.typesafe.akka" %% "akka-slf4j" % akkaVers

    val akka = List(akka_actor,akka_stream,akka_http,akka_actor_typed,akka_slf4j)

    val zio = List(zio_main,zio_conf_magnolia,zio_conf_typesafe,zio_conf,zio_logg,zio_logg_slf4j)

    val circe_libs = Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser",
      "io.circe" %% "circe-literal"
    ).map(_ % circeVers)

  }

val commonDependencies = {
  List(dependencies.logback) ++
    dependencies.zio ++
    List(dependencies.apache_dbcp2) ++
    dependencies.akka ++
    dependencies.circe_libs
}

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "utf-8",
  "-explaintypes",
  "-feature",
  "-unchecked",
  "-language:postfixOps",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Xcheckinit",
  "-Xfatal-warnings"
)

lazy val commonSettings = Seq(
  scalacOptions ++= compilerOptions,
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    Resolver.mavenLocal,
    Resolver.sonatypeRepo("public"),
    Resolver.bintrayRepo("websudos", "oss-releases"),
    Resolver.sonatypeRepo("releases")
  )
)

//paradise for using circe annotations, f.e. @JsonCodec case class DbErrorDesc
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

assembly / assemblyMergeStrategy := {
  case x if x.contains("io.netty.versions.properties") => MergeStrategy.discard
  case PathList("module-info.class") => MergeStrategy.discard
  case x if x.endsWith("/module-info.class") => MergeStrategy.discard
  case PathList("META-INF", xs @ _*)         => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}
