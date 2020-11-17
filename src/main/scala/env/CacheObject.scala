package env

import java.util.concurrent.TimeUnit

import data.{Cache, CacheEntity}
import db.Ucp.UcpZLayer.poolCache
import zio.{Has, Ref, UIO, ZIO, ZLayer, ZManaged}
import env.EnvContainer.{ConfigWsConf, ConfigWsConfClock}
import izumi.reflect.Tag
import stat.StatObject.{CacheCleanElm, CacheGetElm, ConnStat, FixedList, WsStat}
import wsconfiguration.ConfClasses.WsConfig
import zio.clock.Clock
import zio.config.ZConfig

import scala.collection.immutable.IntMap

object CacheObject {

  type CacheManager = Has[CacheManager.Service]

  object CacheManager {

    trait Service {
      def addHeartbeat: UIO[Unit]
      def getCacheValue: UIO[Cache]
      def get(key: Int): UIO[Option[CacheEntity]]
      def set(key: Int, value: CacheEntity): UIO[Unit]
      def remove(keys: Seq[Int]): UIO[Unit]
      def getWsStartTs: UIO[Long]
      def getGetCount: UIO[FixedList[CacheGetElm]]
      def getCleanCount: UIO[FixedList[CacheCleanElm]]
      def getConnStat: UIO[FixedList[ConnStat]]
      def clearGetCounter :UIO[Unit]
      def saveCleanElemsCnt(size: Int) :UIO[Unit]
      def saveConnStats(sizeAvail: Int, sizeBorrow: Int) :UIO[Unit]
      def clearWholeCache: UIO[Unit]
    }

    final class refCache(ref: Ref[Cache], stat: Ref[WsStat]) extends CacheManager.Service {
      override def addHeartbeat: UIO[Unit] =
        ref.update(cv => cv.copy(HeartbeatCounter = cv.HeartbeatCounter + 1))

      override def getCacheValue: UIO[Cache] = ref.get.map(c => c)

      override def get(key: Int): UIO[Option[CacheEntity]] = for {
        ce <- ref.get.map(_.dictsMap.get(key))
        _ <- UIO(ce).flatMap(r =>
          r.fold(UIO.succeed(()))(
            SomeCe => this.set(key, SomeCe.copy(tslru = System.currentTimeMillis))
          )
        )
        _ <- stat.update(
          wss => WsStat(wss.wsStartTs,wss.currGetCnt+1, wss.statGets, wss.statsCleanElems, wss.statsConn)
        )
      } yield ce

      override def clearGetCounter :UIO[Unit] = {
        stat.update(
          wss => {
            val newElem: FixedList[CacheGetElm] = wss.statGets
            wss.statGets.append(CacheGetElm(System.currentTimeMillis, wss.currGetCnt))
            WsStat(wss.wsStartTs, 0, newElem, wss.statsCleanElems, wss.statsConn)
          }
        )
      }

      override def saveCleanElemsCnt(size: Int) :UIO[Unit] = for {
        currCacheElmCnt <- ref.get.map(rc => rc.dictsMap.size)
        _ <- stat.update(
          wss => {
            val newCleanElm : FixedList[CacheCleanElm] = wss.statsCleanElems
            wss.statsCleanElems.append(CacheCleanElm(System.currentTimeMillis, size, currCacheElmCnt-size))
            WsStat(wss.wsStartTs, wss.currGetCnt, wss.statGets, newCleanElm, wss.statsConn)
          }
        )
      } yield ()

      override def saveConnStats(sizeAvail: Int, sizeBorrow: Int) :UIO[Unit] = for {
        _ <- stat.update(
          wss => {
            val newStatElm : FixedList[ConnStat] = wss.statsConn
            wss.statsConn.append(ConnStat(System.currentTimeMillis, sizeAvail, sizeBorrow))
            WsStat(wss.wsStartTs, wss.currGetCnt, wss.statGets, wss.statsCleanElems , newStatElm)
          }
        )
      } yield ()

      override def set(key: Int, value: CacheEntity): UIO[Unit] = {
       ref.update(cv => cv.copy(HeartbeatCounter = cv.HeartbeatCounter + 1, dictsMap = cv.dictsMap + (key -> value)))
    }

      override def remove(keys: Seq[Int]): UIO[Unit] =
        ref.update(cvu => cvu.copy(HeartbeatCounter = cvu.HeartbeatCounter + 1,
          dictsMap = cvu.dictsMap -- keys))

      override def getWsStartTs: UIO[Long] = for {
        startTs <- stat.get.map(_.wsStartTs)
      } yield startTs

      override def getGetCount: UIO[FixedList[CacheGetElm]] = stat.get.map(_.statGets)

      override def getCleanCount: UIO[FixedList[CacheCleanElm]] = stat.get.map(_.statsCleanElems)

      override def getConnStat: UIO[FixedList[ConnStat]] = stat.get.map(_.statsConn)

      override def clearWholeCache: UIO[Unit] = UIO.unit

    }

    def refCache(implicit tag: Tag[CacheManager.Service]
                ): ZLayer[ConfigWsConfClock, Nothing, CacheManager] = {
      {
        val eff: ZIO[ConfigWsConfClock, Nothing, CacheManager.Service] = for {
          cfg <- ZIO.access[ConfigWsConf](_.get.smconf)
          clk <- ZIO.access[Clock](_.get)
          currTs <- clk.currentTime(TimeUnit.MILLISECONDS)
          refInitEmptyCache <- Ref.make(
            WsStat(
            currTs, 0,
            new FixedList[CacheGetElm](cfg.getcntHistoryDeep),
            new FixedList[CacheCleanElm](cfg.getcntHistoryDeep),
            new FixedList[ConnStat](cfg.getcntHistoryDeep)
          )).flatMap(refInitStats =>
            Ref.make(Cache(0, System.currentTimeMillis, IntMap.empty)
            ).map(refInitEmptyCache => new refCache(refInitEmptyCache, refInitStats)))
        } yield refInitEmptyCache
        eff.toLayer
      }
    }

  }
}