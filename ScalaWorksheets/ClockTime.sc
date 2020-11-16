import java.util.concurrent.TimeUnit
import zio.clock.Clock
import zio.console.putStrLn
import zio.console.Console
import zio.duration.durationInt
import zio.{ExitCode, Runtime, ZEnv, ZIO}

val prg: ZIO[Console with Clock, Throwable, Int] = for {

 c <- ZIO.access[Clock](_.get)
 zio_begin <- c.currentTime(TimeUnit.MILLISECONDS)
 zio_beginM <- ZIO.accessM[Clock](_.get.currentTime(TimeUnit.MILLISECONDS))
 sys_begin <- ZIO.succeed(System.currentTimeMillis)
 _ <- putStrLn(s" zio_begin  = $zio_begin")
 _ <- putStrLn(s" zio_beginM = $zio_beginM")
 _ <- putStrLn(s" sys_begin  = $sys_begin")

 _ <- c.sleep(1.second)

 zio_end <- c.currentTime(TimeUnit.MILLISECONDS)
 zio_endM <- ZIO.accessM[Clock](_.get.currentTime(TimeUnit.MILLISECONDS))
 sys_end <- ZIO.succeed(System.currentTimeMillis)
 _ <- putStrLn(s"  zio_end  = $zio_end")
 _ <- putStrLn(s"  zio_endM = $zio_beginM")
 _ <- putStrLn(s"  sys_end  = $sys_end")

 _ <- putStrLn(s" duration zio  = ${zio_end - zio_begin} ms.")
 _ <- putStrLn(s" duration zioM = ${zio_endM - zio_beginM} ms.")
 _ <- putStrLn(s" duration sys  = ${sys_end - sys_begin} ms.")
} yield 1

object MyApp extends App{
   def run(args: List[String]): ZIO[ZEnv,Nothing,ExitCode] =
    prg.provideLayer(ZEnv.live).exitCode
}

val runtime = Runtime.default
runtime.unsafeRun(MyApp.run(List[String]()))