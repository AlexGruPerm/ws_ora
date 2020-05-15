package application

import db.Ucp.UcpZLayer
import env.CacheObject.CacheManager
import env.EnvContainer.ZEnvConfLogCache
import zio.ZIO
import zio.logging.log

object StatHelper {

  val statMonitor: ZIO[ZEnvConfLogCache, Nothing, Unit] =
    for {
      cache <- ZIO.access[CacheManager](_.get)
      startTs <- cache.getWsStartTs
      _ <- log.info(s"~~~~~~~~~~ WS CURRENT STATISTIC ~~~~~~~~~~~~~~~~~")
      _ <- log.info(s"WebService uptime : ${(System.currentTimeMillis - startTs)/1000} seconds.")
      _ <- log.info(s"~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    } yield ()

}
