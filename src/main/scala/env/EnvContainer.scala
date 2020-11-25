package env

import zio.{Has, Layer, ZEnv, ZLayer, config}
import zio.clock.Clock
import zio.console.Console
import CacheObject._
import akka.http.scaladsl.Http.{IncomingConnection, ServerBinding}
import wsconfiguration.ConfClasses.WsConfig
import zio.logging.Logging
import ConfigLayerObject.configLayer
import db.Ucp
import Ucp.UcpZLayer
import zio.config._
import ConfigDescriptor._

import scala.concurrent.Future

object EnvContainer {
  type IncConnSrvBind = akka.stream.scaladsl.Source[IncomingConnection, Future[ServerBinding]]

  type ConfigWsConf  = ZConfig[WsConfig]
  type ConfigWsConfClock = ConfigWsConf with Clock
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
    val confLayer: Layer[Throwable, ConfigWsConf] = configLayer(confFileName)
    val confLayerWithClock : Layer[Throwable, ConfigWsConfClock] = confLayer ++ Clock.live
    val combEnvWithoutCache = ZEnv.live ++ envLog ++ confLayer ++ (confLayerWithClock >>> CacheManager.refCache)
    combEnvWithoutCache ++ (combEnvWithoutCache >>> Ucp.UcpZLayer.poolCache)
  }

}
