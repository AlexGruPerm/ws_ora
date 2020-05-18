package application

import db.Ucp.UcpZLayer
import env.CacheObject.CacheManager
import env.EnvContainer.ZEnvConfLogCache
import stat.StatObject.WsStat
import zio.{Task, ZIO}
import zio.logging.log

object StatHelper {

  val statMonitor: ZIO[ZEnvConfLogCache, Nothing, Unit] =
    for {
      cache <- ZIO.access[CacheManager](_.get)
      startTs <- cache.getWsStartTs
      gc <- cache.getGetCount
      _ <- log.info(s"~~~~~~~~~~ WS CURRENT STATISTIC ~~~~~~~~~~~~~~~~~")
      _ <- log.info(s"uptime : ${(System.currentTimeMillis - startTs)/1000} sec. Get(count)=$gc")
      _ <- cache.clearGetCounter
      _ <- ZIO.foreach(gc.toList)(elm => log.info(s" ELM : ${elm.ts} - ${elm.cnt}"))
      _ <- ZIO.succeed(gc.foreach(elm => log.info(s"    ELM : ${elm.ts} - ${elm.cnt}")))
      _ <- log.info(s"~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    } yield ()

}
