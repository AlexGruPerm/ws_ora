import zio._
import zio.console._
import zio.config.magnolia.DeriveConfigDescriptor._
import zio.ZLayer._
import zio.config.Config
import zio.config.typesafe.TypesafeConfig
import zio.config.typesafe.TypeSafeConfigSource
import zio.console.Console
import zio.{App, ZEnv, ZIO}
//import zio.config.Config


final case class ApiConfig(endpoint: String, port: Int)
final case class DbConfig(
                           ip: String,
                           port: Int = 1527,
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

/*
final case class WsConfig(
                           ip: String,
                           port: Int = 1525,
                           sid: String,
                           username: String,
                           password: String
                         )
*/

val wsConfigAutomatic = descriptor[WsConfig]

val configLayer :Layer[Throwable, config.Config[WsConfig]] =
  TypesafeConfig.fromHoconFile(new java.io.File("C:\\ws_ora\\src\\main\\resources\\application.conf"),
    wsConfigAutomatic)

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

  val appLayer = ZEnv.live ++ configLayer

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {

    val prg = for {
      _  <- finalExecution.provideCustomLayer(appLayer)
    } yield ()
    prg.fold(_ => 1, _ => 0)
  }
}

val runtime = Runtime.default
runtime.unsafeRun(MyApp.run(List()).provideLayer(MyApp.appLayer))