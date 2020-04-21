package env

import data.{Cache, CacheEntity, DictDataRows}
import zio.{Has, Ref, Tagged, UIO, ZLayer}

object CacheObject {

  type CacheManager = Has[CacheManager.Service]

  object CacheManager {

    trait Service {
      def addHeartbeat: UIO[Unit]
      def getCacheValue: UIO[Cache]
      def get(key: Int): UIO[Option[CacheEntity]]
      def set(key: Int, value: CacheEntity): UIO[Unit]
      def remove(keys: Seq[Int]): UIO[Unit]
    }

    final class refCache(ref: Ref[Cache]) extends CacheManager.Service {
      override def addHeartbeat: UIO[Unit] =
        ref.update(cv => cv.copy(HeartbeatCounter = cv.HeartbeatCounter + 1))

      override def getCacheValue: UIO[Cache] = ref.get.map(c => c)

      override def get(key: Int): UIO[Option[CacheEntity]] = ref.get.map(_.dictsMap.get(key))

      override def set(key: Int, value: CacheEntity): UIO[Unit] = {
       //println(s"METHOD SET - CURR HBC = ${ref.get.map(_.HeartbeatCounter)}")
       ref.update(cv => cv.copy(HeartbeatCounter = cv.HeartbeatCounter + 100,
          dictsMap = cv.dictsMap + (key -> value)))
    }

      override def remove(keys: Seq[Int]): UIO[Unit] =
        ref.update(cvu => cvu.copy(HeartbeatCounter = cvu.HeartbeatCounter + 1,
          dictsMap = cvu.dictsMap -- keys))
    }

    def refCache(implicit tag: Tagged[CacheManager.Service]
                      ): ZLayer[Any, Nothing, CacheManager] = {
      ZLayer.fromEffect[Any,
        Nothing,
        CacheManager.Service
      ]{
        Ref.make(Cache(0, System.currentTimeMillis,
          Map(1 -> CacheEntity(DictDataRows("empty", 0L, 0L, 0L, List(List())),Seq()))))
          .map(refEmpty => new refCache(refEmpty))
      }
    }

  }
}