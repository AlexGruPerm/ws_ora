package application

import env.CacheObject.CacheManager
import env.EnvContainer.{ConfigWsConf, ZEnvLogCache}
import zio.logging.log
import zio.{Task, UIO, ZIO}

import scala.language.postfixOps

object CacheClean {
  val checkAndClean : ZIO[ZEnvLogCache, Nothing, Unit] = for {
    cache <- ZIO.access[CacheManager](_.get)
    conf <- ZIO.access[ConfigWsConf](_.get)
    cvals <- cache.getCacheValue
    currTs = System.currentTimeMillis
    entityForClean <- ZIO.filter(cvals.dictsMap)(ce =>
      UIO(ce._1!=1 && (currTs - ce._2.tslru) > conf.smconf.ltUnusedCache*1000L)
    )
    _ <- ZIO.foreach(entityForClean)(
      te => log.info(s" ####### need to clean key : ${te._1} #######")
    )
    _ <- cache.saveCleanElemsCnt(entityForClean.size)
    //todo: add here saving cnt elemtns in Cache, like in CacheCleanElm. look as StatObject and add new c.c.
    /* SIZE from here:
          cv <- cache.getCacheValue cv.dictsMap.size
    */
    _ <- cache.remove(entityForClean.toSeq.map(tup => tup._1))
  } yield ()
}
