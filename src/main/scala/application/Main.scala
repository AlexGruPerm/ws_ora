package application

import application.Main.checkArgs
import env.CacheAsZLayer.CacheManager
import env.ConfigLayerObject.configLayer
import zio.{App, Layer, Runtime, Task, UIO, ZEnv, ZIO, ZLayer}
import env.EnvContainer._
import wsconfiguration.ConfClasses.WsConfig
//import wsconfiguration.ConfClasses.WsConfig
import zio.config.Config
import zio.logging._
import zio.console.{Console, putStrLn}
import logs.LogHelpers._
import scala.language.higherKinds


// C:\ws_ora\src\main\resources\application.conf
object Main extends App {



  private val wsApp: ZIO[ZEnvConfLogCache, Throwable, Unit] =
    for {
      _ <- log.info("Web service starting")
      _ <- outputInitalConfig
      //res <- WebService.startService(cfg)
      _ <- log.info("Web service stopping")
    } yield ()//res

  private val rt: List[String] => Runtime.Managed[ZEnvConfLogCache] = args => Runtime.unsafeFromLayer(
    ZEnv.live >>> env.EnvContainer.ZEnvConfLogCacheLayer(args.head)
  )

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val isArgsNotEmpty: Boolean = Runtime.global.unsafeRun(checkArgs(args))
    if (isArgsNotEmpty) {
      ZIO(rt(args).unsafeRun(wsApp)).foldM(
        throwable => putStrLn(s"Error: ${throwable.getMessage}") *>
          ZIO.foreach(throwable.getStackTrace) { sTraceRow =>
            putStrLn(s"$sTraceRow")
          } as 1,
        _ => putStrLn(s"Success exit of application.") as 0
      )
    } else {
      log.error("[WS-0001] Need input config file as parameter.").provideLayer(envLog) *> Task.succeed(1)
    }
  }

  private def checkArgs(args: List[String]): Task[Boolean] =
    if (args.isEmpty)
      UIO.succeed(false)
    else
      UIO.succeed(true)

}