package application

import env.CacheObject.CacheManager
import env.EnvContainer.ZEnvConfLogCache
import zio.ZIO
import zio.logging.log

object StatHelper {

  val statMonitor: ZIO[ZEnvConfLogCache, Nothing, Unit] =
    for {
      cache <- ZIO.access[CacheManager](_.get)
      startTs <- cache.getWsStartTs
      gc <- cache.getGetCount
      cc <- cache.getCleanCount
      cs <- cache.getConnStat

      _ <- log.trace(s"~~~~~~~~~~ WS CURRENT STATISTIC ~~~~~~~~~~~~~~~~~")
      _ <- log.trace(s"uptime : ${(System.currentTimeMillis - startTs)/1000} sec. Get(count) = ${gc.size}")
      _ <- cache.clearGetCounter

      _ <- ZIO.foreach(gc.toList)(
        elm => log.trace(s" CacheGets : ${elm.ts} - ${elm.cnt}"))

      _ <- ZIO.foreach(cc.toList)(
        elm => log.trace(s" CacheCleaning : ${elm.ts} - Cleared: ${elm.cntCleared} Existing: ${elm.cntExisting}"))

      _ <- ZIO.foreach(cs.toList)(
        elm => log.trace(
          s" ConnectStat : ${elm.ts} - Available: ${elm.AvailableConnCnt} Borrowed: ${elm.BorrowedConnCnt}"))

      _ <- log.info(s"~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    } yield ()

}
