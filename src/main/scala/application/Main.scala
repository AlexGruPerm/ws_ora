package application

import zio.{App, ExitCode, Runtime, Task, UIO, ZEnv, ZIO}
import env.EnvContainer._
import zio.logging._
import zio.console.putStrLn
import logs.LogHelpers._

import scala.language.higherKinds

// C:\ws_ora\src\main\resources\application.conf
object Main extends App {

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    val isArgsNotEmpty: Boolean = Runtime.global.unsafeRun(checkArgs(args))
    if (isArgsNotEmpty) {
      ZIO(rt(args).unsafeRun(wsApp)).foldM(
        throwable => putStrLn(s"Error: ${throwable.getMessage}") *>
          ZIO.foreach(throwable.getStackTrace) { sTraceRow =>
            putStrLn(s"$sTraceRow")
          } as ExitCode.failure,
        _ => putStrLn(s"Success exit of application.") as ExitCode.success
      )
    } else {
      log.error("[WS-0001] Need input config file as parameter.").provideLayer(envLog) as ExitCode.failure//Task.succeed(1)
    }
  }

  private val rt: List[String] => Runtime.Managed[ZEnvConfLogCache] = args => Runtime.unsafeFromLayer(
    ZEnv.live >>> env.EnvContainer.ZEnvConfLogCacheLayer(args.head)
  )

  private val wsApp: ZIO[ZEnvConfLogCache, Throwable, Unit] =
    for {
      _ <- log.info("Web service starting")
      _ <- outputInitalConfig
      res <- WebService.startService
      _ <- log.info("Web service stopping")
    } yield res

  private def checkArgs(args: List[String]): Task[Boolean] =
    if (args.isEmpty)
      UIO.succeed(false)
    else
      UIO.succeed(true)

}