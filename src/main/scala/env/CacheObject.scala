package env

import data.{Cache, CacheEntity, DictDataRows}
import zio.logging.Logging.Logging
import zio.logging.log
import zio.{Has, Ref, Tagged, UIO, URIO, ZIO, ZLayer, ZManaged, clock, config}
import java.util.concurrent.TimeUnit

import env.EnvContainer.ConfigWsConf
import stat.StatObject.{CacheGetElm, FixedList, WsStat}
import wsconfiguration.ConfClasses.WsConfig
import zio.clock.currentTime
import zio.config.Config

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
      def clearGetCounter :UIO[Unit]
    }

    final class refCache(ref: Ref[Cache], stat: Ref[WsStat]) extends CacheManager.Service {
      override def addHeartbeat: UIO[Unit] =
        ref.update(cv => cv.copy(HeartbeatCounter = cv.HeartbeatCounter + 1))

      override def getCacheValue: UIO[Cache] = ref.get.map(c => c)

      override def get(key: Int): UIO[Option[CacheEntity]] = for {
        _ <- ref.get.map(_.dictsMap.get(key)).flatMap(r =>
          r.fold(UIO.succeed(()))(
            SomeCe => this.set(key, SomeCe.copy(tslru = System.currentTimeMillis))
          )
        )
        _ <- stat.update(
          wss => WsStat(wss.wsStartTs,wss.currGetCnt+1, wss.statGets)
        )
        r <- ref.get.map(_.dictsMap.get(key))
      } yield r

      override def clearGetCounter :UIO[Unit] = {
        stat.update(
          wss => {
            val newElem: FixedList[CacheGetElm] = wss.statGets
            wss.statGets.append(CacheGetElm(System.currentTimeMillis, wss.currGetCnt))
            WsStat(wss.wsStartTs, 0, newElem)
          }
        )
      }

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

    }

    def refCache(implicit tag: Tagged[CacheManager.Service]
                           ): ZLayer[ConfigWsConf, Nothing, CacheManager] = {
        ZLayer.fromEffect[
          ConfigWsConf,//Any,
        Nothing,
        CacheManager.Service
      ] {
          ZManaged.access[Config[WsConfig]](_.get).use(wscfg =>
            Ref.make(WsStat(System.currentTimeMillis, 0, new FixedList[CacheGetElm](
              wscfg.smconf.getcntHistoryDeep))).flatMap(sts =>
              Ref.make(
                Cache(0, System.currentTimeMillis,
                  Map(1 -> CacheEntity(
                    System.currentTimeMillis, System.currentTimeMillis,
                    DictDataRows("empty", 0L, 0L, 0L, List(List())), Seq()))
                )
              ).map(refEmpty => new refCache(refEmpty, sts))
            )
          )
      }
    }

  }
}