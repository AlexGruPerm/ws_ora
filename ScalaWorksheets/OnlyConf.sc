import java.io.File
import zio._
import zio.console._
import zio.config.magnolia.DeriveConfigDescriptor._
import zio.ZLayer._
import zio.config.Config
import zio.config.typesafe.TypesafeConfig
import zio.console.Console
import zio.{App, ZEnv, ZIO}

final case class ApiConfig(endpoint: String, port: Int)

final case class DbConfig(
                           ip: String,
                           port: Int = 1521,
                           sid: String,
                           username: String,
                           password: String
                         )

final case class UcpConfig(
                            ConnectionPoolName: String,
                            InitialPoolSize: Int ,
                            MinPoolSize: Int ,
                            MaxPoolSize: Int,
                            ConnectionWaitTimeout: Int
                          )

final case class WsConfig(api: ApiConfig, dbconf: DbConfig, ucpconf: UcpConfig)

val wsConfigAutomatic = descriptor[WsConfig]

def configLayer(confFileName: String): Layer[Throwable, config.Config[WsConfig]] =
  TypesafeConfig.fromHoconFile(new java.io.File(confFileName), wsConfigAutomatic)

val finalExecution: ZIO[Console with Config[WsConfig], Nothing, Unit] =
  for {
    wsCommConfig <- ZIO.access[Config[WsConfig]](_.get)
    _         <- putStrLn(" ~~~~~~~~~~~~~~~~~ DB ~~~~~~~~~~~~~~~~~~~~~ ")
    _         <- putStrLn(s" sid  = ${wsCommConfig.dbconf.sid}")
    _         <- putStrLn(s" ip   = ${wsCommConfig.dbconf.ip}")
    _         <- putStrLn(s" port = ${wsCommConfig.dbconf.port}")
    _         <- putStrLn(" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ")
  } yield ()


object MyApp extends App {
  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val args_head: String = "C:\\ws_ora\\src\\main\\resources\\application.conf"
    val appLayer = ZEnv.live ++ configLayer(args_head)
    val prg = for {
      _  <- finalExecution.provideCustomLayer(appLayer)
    } yield ()
    prg.fold(_ => 1, _ => 0)
  }
}

val runtime = Runtime.default
runtime.unsafeRun(MyApp.run(List()))