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
import scala.language.higherKinds


object Main extends App {

  private def checkArgs(args: List[String]): Task[Unit] =
    if (args.isEmpty) {
      Task.fail(new IllegalArgumentException("[WS-0001] Need input config file as parameter."))
    } else {
      UIO.succeed(())
    }

  private def wsApp: List[String] => ZIO[ZEnvConfLogCache, Throwable, Unit] = args =>
    for {
      _ <- log.info("Web service starting")
      cfg <- ZIO.access[Config[WsConfig]](_.get)
      _         <- log.info(" ~~~~~~~~~~~~~~~~~ DB ~~~~~~~~~~~~~~~~~~~~~ ")
      _         <- log.info(s" sid  = ${cfg.dbconf.sid}")
      _         <- log.info(s" ip   = ${cfg.dbconf.ip}")
      _         <- log.info(s" port = ${cfg.dbconf.port}")
      _         <- log.info(" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ")
      //cfg <-  ZIO.access[Configuration](_.get.load("C:\\ws_fphp\\src\\main\\resources\\application.conf"))
      //res <- WebService.startService(cfg)
      _ <- log.info("Web service stopping")
    } yield ()//res


  // C:\ws_ora\src\main\resources\application.conf
  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val argsCheckResult = Runtime.global.unsafeRun(checkArgs(args))
    val appLayer = ZEnv.live >>> env.EnvContainer.ZEnvConfLogCacheLayer(args.head)
    val rt: Runtime.Managed[ZEnvConfLogCache] = Runtime.unsafeFromLayer(appLayer)
    ZIO(rt.unsafeRun(wsApp(args))).foldM(throwable => putStrLn(s"Error: ${throwable.getMessage}") *>
      ZIO.foreach(throwable.getStackTrace) { sTraceRow =>
        putStrLn(s"$sTraceRow")
      } as 1,
      _ => putStrLn(s"Success exit of application.") as 0
    )

  }



}