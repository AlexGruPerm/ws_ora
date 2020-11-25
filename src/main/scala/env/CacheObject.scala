package env

import java.util.concurrent.TimeUnit
import data.{Cache, CacheEntity}
import zio.{Has, Ref, UIO, URIO, ZIO, ZLayer}
import env.EnvContainer.{ConfigWsConf, ConfigWsConfClock}
import izumi.reflect.Tag
import stat.StatObject.{CacheCleanElm, CacheGetElm, ConnStat, FixedList, WsStat}
import zio.clock.Clock
import scala.collection.immutable.IntMap

object CacheObject {

  type CacheManager = Has[CacheManager.Service]

  object CacheManager {

    trait Service {
      def addHeartbeat: UIO[Unit]
      def getCacheValue: UIO[Cache]
      def get(key: Int): URIO[Clock,Option[CacheEntity]]
      def set(key: Int, value: CacheEntity): UIO[Unit]
      def remove(keys: Seq[Int]): UIO[Unit]
      def getWsStartTs: UIO[Long]
      def getGetCount: UIO[FixedList[CacheGetElm]]
      def getCleanCount: UIO[FixedList[CacheCleanElm]]
      def getConnStat: UIO[FixedList[ConnStat]]
      def clearGetCounter :URIO[Clock, Unit]
      def saveCleanElemsCnt(size: Int) :URIO[Clock, Unit]
      def saveConnStats(sizeAvail: Int, sizeBorrow: Int): URIO[Clock, Unit]
      def clearWholeCache: UIO[Unit]
    }

    final class refCache(ref: Ref[Cache], stat: Ref[WsStat]) extends CacheManager.Service {
      override def addHeartbeat: UIO[Unit] =
        ref.update(cv => cv.copy(HeartbeatCounter = cv.HeartbeatCounter + 1))

      override def getCacheValue: UIO[Cache] = ref.get.map(c => c)

      override def get(key: Int): URIO[Clock,Option[CacheEntity]] = for {
        currTs <- ZIO.accessM[Clock](_.get.currentTime(TimeUnit.MILLISECONDS))
        ce <- ref.get.map(_.dictsMap.get(key))
        _ <- UIO(ce).flatMap(r =>
          r.fold(UIO.succeed(()))(
            SomeCe => this.set(key, SomeCe.copy(tslru = currTs))
          )
        )
        _ <- stat.update(
          wss => WsStat(wss.wsStartTs,wss.currGetCnt+1, wss.statGets, wss.statsCleanElems, wss.statsConn)
        )
      } yield ce

      override def clearGetCounter :URIO[Clock, Unit] = for {
        currTs <- ZIO.accessM[Clock](_.get.currentTime(TimeUnit.MILLISECONDS))
        _ <- stat.update(
          wss => {
            val newElem: FixedList[CacheGetElm] = wss.statGets
            wss.statGets.append(CacheGetElm(currTs, wss.currGetCnt))
            WsStat(wss.wsStartTs, 0, newElem, wss.statsCleanElems, wss.statsConn)
          }
        )
      } yield ()

      override def saveCleanElemsCnt(size: Int) :URIO[Clock, Unit] = for {
        currTs <- ZIO.accessM[Clock](_.get.currentTime(TimeUnit.MILLISECONDS))
        currCacheElmCnt <- ref.get.map(rc => rc.dictsMap.size)
        _ <- stat.update(
          wss => {
            val newCleanElm : FixedList[CacheCleanElm] = wss.statsCleanElems
            wss.statsCleanElems.append(CacheCleanElm(currTs, size, currCacheElmCnt-size))
            WsStat(wss.wsStartTs, wss.currGetCnt, wss.statGets, newCleanElm, wss.statsConn)
          }
        )
      } yield ()

      override def saveConnStats(sizeAvail: Int, sizeBorrow: Int) :URIO[Clock, Unit] = for {
        currTs <- ZIO.accessM[Clock](_.get.currentTime(TimeUnit.MILLISECONDS))
        _ <- stat.update(
          wss => {
            val newStatElm : FixedList[ConnStat] = wss.statsConn
            wss.statsConn.append(ConnStat(currTs, sizeAvail, sizeBorrow))
            WsStat(wss.wsStartTs, wss.currGetCnt, wss.statGets, wss.statsCleanElems , newStatElm)
          }
        )
      } yield ()

      override def set(key: Int, value: CacheEntity): UIO[Unit] =
       ref.update(cv => cv.copy(HeartbeatCounter = cv.HeartbeatCounter + 1, dictsMap = cv.dictsMap + (key -> value)))

      override def remove(keys: Seq[Int]): UIO[Unit] =
        ref.update(cvu => cvu.copy(HeartbeatCounter = cvu.HeartbeatCounter + 1,
          dictsMap = cvu.dictsMap -- keys))

      override def getWsStartTs: UIO[Long] = stat.get.map(_.wsStartTs)

      override def getGetCount: UIO[FixedList[CacheGetElm]] = stat.get.map(_.statGets)

      override def getCleanCount: UIO[FixedList[CacheCleanElm]] = stat.get.map(_.statsCleanElems)

      override def getConnStat: UIO[FixedList[ConnStat]] = stat.get.map(_.statsConn)

      override def clearWholeCache: UIO[Unit] = UIO.unit

    }

    def refCache(implicit tag: Tag[CacheManager.Service]): ZLayer[ConfigWsConfClock, Nothing, CacheManager] = {
      val eff: ZIO[ConfigWsConfClock, Nothing, CacheManager.Service] = for {
        cfg <- ZIO.access[ConfigWsConf](_.get.smconf)
        clk <- ZIO.access[Clock](_.get)
        currTs <- clk.currentTime(TimeUnit.MILLISECONDS)
        refInitWsStat <- Ref.make(WsStat(currTs, cfg.getcntHistoryDeep))
        refInitEmpCache <- Ref.make(Cache(0, currTs, IntMap.empty))
        cache = new refCache(refInitEmpCache, refInitWsStat)
      } yield cache
      eff.toLayer
    }

  }
}