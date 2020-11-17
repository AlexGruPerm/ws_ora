import java.util.concurrent.TimeUnit
import zio.clock.Clock
import zio.console.{Console, putStrLn}
import zio.{App, ExitCode, Has, Layer, Ref, Runtime, UIO, ZEnv, ZIO, ZLayer}

import scala.collection.immutable.IntMap

//------ C.C. ------------
object CommonCC{
  case class CacheEntity(tscreate: Long, reftables: Seq[String], ip: String)
  case class Cache(
                    cacheCreatedTs: Long = System.currentTimeMillis,
                    dictsMap: IntMap[CacheEntity]
                  )
}

//------- Module 1 has no dependencies ---------
object CommonObjectCfg {
  type Conf = Has[Conf.Service]
  object Conf {
    trait Service {
      def getIp: UIO[String]
    }
    case class ConfServImpl(fn: String) extends Conf.Service {
      override def getIp: UIO[String] = UIO("127.0.0.1")
    }
    def live(fn: String): ZLayer[Any, Nothing, Conf] = {
      ZLayer.succeed(ConfServImpl(fn))
    }
  }
}

//---- Module 2 has dependencies Conf and Clock ----
import CommonCC._
import CommonObjectCfg.Conf
object CommonObjCacheObject{
  type CacheManager = Has[CacheManager.Service]
  object CacheManager {

    trait Service {
      def set(key: Int, value: CacheEntity): UIO[Unit]
      def get(key: Int): UIO[Option[CacheEntity]]
      def clearAll: UIO[Unit] = UIO.unit
    }

    final class refCache(ref: Ref[Cache], cfg: String) extends CacheManager.Service {
      override def set(key: Int, value: CacheEntity): UIO[Unit] =
        ref.update(cv => cv.copy(dictsMap = cv.dictsMap + (key -> value)))

      override def get(key: Int): UIO[Option[CacheEntity]] = for {
        ce <- ref.get.map(_.dictsMap.get(key))
        _ <- UIO(ce).flatMap(r =>
          r.fold(UIO.succeed(()))(
            SomeCe => this.set(key, SomeCe.copy(System.currentTimeMillis))
          )
        )
      } yield ce
    }

    def refCache: ZLayer[Conf with Clock, Nothing, CacheManager] = {
      val eff: ZIO[Conf with Clock, Nothing, refCache] = for {
        cfg <- ZIO.access[Conf](_.get)
        clk <- ZIO.access[Clock](_.get)
        currTs <- clk.currentTime(TimeUnit.MILLISECONDS)
        c = Cache(currTs, IntMap.empty)
        ec <- Ref.make(c)
        ip <- cfg.getIp
        zm = new refCache(ec,ip)
      } yield zm
      eff.toLayer
    }

  }

}

import CommonObjCacheObject.CacheManager
object env{
  type ZEnvConfClock =  Conf with Clock
  def ZEnvConfLogCacheLayer(confFileName: String): Layer[Throwable, CacheManager] = {
    val confLayer: Layer[Throwable, Conf] = Conf.live(confFileName)
    val confLayerWithClock : Layer[Throwable, ZEnvConfClock] = confLayer ++ Clock.live
    val r : Layer[Throwable, CacheManager] = confLayerWithClock >>> CacheManager.refCache
    r
  }
}

val appLayer :Layer[Throwable, CacheManager] =  env.ZEnvConfLogCacheLayer("filename")

val prg :ZIO[CacheManager with ZEnv,Throwable,Int] = for {
  _ <- putStrLn("Begin prg")
  cm <- ZIO.access[CacheManager](_.get)
  clk <- ZIO.access[Clock](_.get)
  currtm <- clk.currentTime(TimeUnit.MILLISECONDS)
  _ <- cm.set(1, CacheEntity(currtm,Seq("t1","t1"),""))
  _ <- cm.set(2, CacheEntity(currtm,Seq("t2","t2"),""))
  _ <- cm.set(3, CacheEntity(currtm,Seq("t3","t3"),""))
  ce3 <- cm.get(3)
  _ <- putStrLn(s"[3] = $ce3")
  ce1 <- cm.get(1)
  _ <- putStrLn(s"[1] = $ce1")
  _ <- putStrLn("End prg")
} yield 1

object MyApp extends App {
  override def run(args: List[String]) :ZIO[ZEnv,Nothing,ExitCode] = {
    prg.provideLayer(appLayer ++ ZEnv.live).exitCode
  }
}

val runtime = Runtime.default
runtime.unsafeRun(MyApp.run(List()))



