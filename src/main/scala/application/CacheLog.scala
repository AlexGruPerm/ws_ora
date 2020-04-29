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
      _ <- log.trace(s"[$effName] HeartbeatCounter = ${cv.HeartbeatCounter} " +
        s"bornTs = ${cv.cacheCreatedTs} dictsMap.size = ${cv.dictsMap.size}")
      _ <- ZIO.foreach(cv.dictsMap)(ce => log.trace(s"Cache element key=${ce._1}  ts=${ce._2.tscreate}"))
      _ <- if (incrHeadrbeat) {
        cache.addHeartbeat
      } else {
        ZIO.succeed(())
      }
    } yield ()
}
