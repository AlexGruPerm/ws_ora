package application


import env.CacheObject.CacheManager
import env.EnvContainer.ZEnvLogCache
import zio._
import zio.logging.log

import scala.language.postfixOps

object CacheLog {
  val out: (String, Boolean) => ZIO[ZEnvLogCache, Nothing, Unit] = (effName, incrHeadrbeat) =>
    for {
      cache <- ZIO.access[CacheManager](_.get)
      cv <- cache.getCacheValue
      _ <- log.trace(s"[$effName] hb : ${cv.HeartbeatCounter} bts : ${cv.cacheCreatedTs} size : ${cv.dictsMap.size}")
      /*
      _ <- log.trace(s"[$effName] HeartbeatCounter = ${cv.HeartbeatCounter} " +
        s"bornTs = ${cv.cacheCreatedTs} dictsMap.size = ${cv.dictsMap.size}")
      */
      /*
      todo: open it somewhere for debug purpose
      _ <- ZIO.foreach(cv.dictsMap)(ce =>
        if (ce._1 != 1) {
          log.trace(s"    [Cache element] key ${ce._1}") *>
          log.trace(s"    tscreate  ${ce._2.tscreate}") *>
          log.trace(s"    tslru     ${ce._2.tslru}")
        }
        else {
          UIO.succeed(())
        }
      )
      */
      _ <- if (incrHeadrbeat) {
        cache.addHeartbeat
      } else {
        ZIO.succeed(())
      }
    } yield ()
}
