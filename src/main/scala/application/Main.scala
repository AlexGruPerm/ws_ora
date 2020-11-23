package application

import com.typesafe.config.ConfigException.IO
import db.Ucp.UcpZLayer
import zio.{App, ExitCode, Runtime, Task, UIO, URIO, ZEnv, ZIO}
import env.EnvContainer._
import zio.logging._
import zio.console.putStrLn
import logs.LogHelpers._

import scala.language.higherKinds

// C:\ws_ora\src\main\resources\application.conf
object Main extends App {

  private def checkArgs(args: List[String]): UIO[Boolean] =
    if (args.isEmpty)
      UIO.succeed(false)
    else
      UIO.succeed(true)

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    ZIO.fromOption(args.headOption)
      .mapError(_ => "[WS-0001] Need input config file as parameter.").foldM(f =>
      log.error(f).provideLayer(envLog) as ExitCode.failure,
      inpFirstParam => ZIO(rt(List(inpFirstParam)).unsafeRun(wsApp)).foldM(
        throwable => putStrLn(s"Error: ${throwable.getMessage}") *>
          ZIO.foreach(throwable.getStackTrace) { sTraceRow =>
            putStrLn(s"$sTraceRow")
          } as ExitCode.failure,
        _ => putStrLn(s"Success exit of application.") as ExitCode.success
      ))
  }

  private val rt: List[String] => Runtime.Managed[ZEnvConfLogCache] = args =>
    Runtime.unsafeFromLayer(
    ZEnv.live >>> env.EnvContainer.ZEnvConfLogCacheLayer(args.head)
  )

  private val wsApp: ZIO[ZEnvConfLogCache, Throwable, Unit] =
    for {
      _ <- log.info("Web service starting")
      ucp <- ZIO.access[UcpZLayer](_.get)
      jdbcVers <- ucp.getJdbcVersion
      _ <- log.info(s"JdbcVersion - $jdbcVers")
      _ <- outputInitalConfig
      res <- WebService.startService
      _ <- log.info("Web service stopping")
    } yield res



}