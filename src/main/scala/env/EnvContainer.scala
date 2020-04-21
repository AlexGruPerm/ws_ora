package env

import zio.{ZEnv, ZLayer, config}
import zio.clock.Clock
import zio.console.Console
import CacheAsZLayer._
import akka.http.scaladsl.Http.{IncomingConnection, ServerBinding}
import wsconfiguration.ConfClasses.WsConfig
import zio.logging.Logging
import zio.logging.Logging.Logging
import ConfigLayerObject.configLayer

import scala.concurrent.Future


object EnvContainer {
  type IncConnSrvBind = akka.stream.scaladsl.Source[IncomingConnection, Future[ServerBinding]]

  type ZEnvLog = ZEnv with Logging
  type ZEnvLogCache =  ZEnvLog with CacheManager
  type ZEnvConfLogCache =  ZEnvLogCache with config.Config[WsConfig]

   val envLog: ZLayer[Console with Clock, Nothing, Logging]   =
    Logging.console((_, logEntry) =>
      logEntry
    )

  val ZEnvLogLayer:  ZLayer[ZEnv, Nothing, ZEnvLog] = ZEnv.live ++ envLog

  val ZEnvLogCacheLayer: ZLayer[ZEnv, Nothing, ZEnvLogCache] =
    ZEnv.live ++ envLog ++ CacheManager.refCache

  def ZEnvConfLogCacheLayer(confFileName: String): ZLayer[ZEnv, Throwable, ZEnvConfLogCache] =
    ZEnv.live ++ envLog ++ configLayer(confFileName) ++ CacheManager.refCache

}
