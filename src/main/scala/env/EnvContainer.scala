package env

import zio.{Has, ZEnv, ZLayer, config}
import zio.clock.Clock
import zio.console.Console
import CacheObject._
import akka.http.scaladsl.Http.{IncomingConnection, ServerBinding}
import wsconfiguration.ConfClasses.WsConfig
import zio.logging.Logging
import zio.logging.Logging.Logging
import ConfigLayerObject.configLayer
import db.Ucp
import Ucp.UcpZLayer
import zio.config.Config

import scala.concurrent.Future

object EnvContainer {
  type IncConnSrvBind = akka.stream.scaladsl.Source[IncomingConnection, Future[ServerBinding]]

  type ConfigWsConf  = Config[WsConfig]

  type ZEnvLog = ZEnv with Logging
  type ZEnvLogCache =  ZEnvLog with ConfigWsConf with CacheManager
  type ZenvLogConfCache_ =  ZEnvLogCache
  type ZEnvConfLogCache =  ZEnvLogCache with UcpZLayer

   val envLog: ZLayer[Console with Clock, Nothing, Logging]   =
    Logging.console((_, logEntry) =>
      logEntry
    )

  val ZEnvLogLayer:  ZLayer[ZEnv, Nothing, ZEnvLog] = ZEnv.live ++ envLog

  def ZEnvConfLogCacheLayer(confFileName: String): ZLayer[ZEnv, Throwable, ZEnvConfLogCache] = {
    val confLayer = configLayer(confFileName)
    val combEnvWithoutPool = ZEnv.live ++ envLog ++ confLayer ++ (confLayer >>> CacheManager.refCache)
    combEnvWithoutPool ++ (combEnvWithoutPool >>> Ucp.UcpZLayer.poolCache)
  }

}
