package application

import db.Ucp.UcpZLayer
import zio.{App, ExitCode, ZEnv, ZIO, ZLayer}
import env.EnvContainer._
import zio.logging._
import zio.console.putStrLn
import logs.LogHelpers._
import scala.language.higherKinds

/**
 * 1) When run(args: List[String]) method is running it takes first input parameter from args: List[String].
 *    It must be full path to config file.
 * 2) And pass it into appLayer thar read it and build full environment (look at ZEnvConfLogCacheLayer),
 *    create Config,Logging,CacheManager,Connection Pool., that available for the whole application
 *    with ZLayer.
 * 3) And further this ZLayer used as input variable in wsApp
*/
object Main extends App {

  private val appLayer: String => ZLayer[Any,Throwable,ZEnvConfLogCache] = confFile =>
    ZEnv.live >>> env.EnvContainer.ZEnvConfLogCacheLayer(confFile)

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

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    ZIO.fromOption(args.headOption)
      .mapError(_ => "[WS-0001] Need input config file as parameter.").foldM(f =>
      log.error(f).provideLayer(envLog) as ExitCode.failure,
      firstParam => wsApp.provideLayer(appLayer(firstParam))
        .foldM(
        throwable => putStrLn(s"Error: ${throwable.getMessage}") *>
          ZIO.foreach(throwable.getStackTrace) { sTraceRow =>
            putStrLn(s"$sTraceRow")
          } as ExitCode.failure,
        _ => putStrLn(s"Success exit of application.") as ExitCode.success
      ))
  }

}