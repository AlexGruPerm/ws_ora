import zio._
import zio.console._
import zio.config.magnolia.DeriveConfigDescriptor.descriptor
import zio.config._
import zio.ZLayer._
import zio.config.Config
import zio.console.Console
import zio.{App, ZEnv, ZIO}

/*
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
final case class WsConfig(api: ApiConfig, dbConfig: DbConfig, ucpcfg: UcpConfig)
*/

final case class WsConfig(
                           ip: String,
                           port: Int = 1521,
                           sid: String,
                           username: String,
                           password: String
                         )

val wsConfigAutomatic = descriptor[WsConfig]

val configLayer = Config.fromPropertiesFile("C:\\ws_ora\\src\\main\\resources\\application.conf", wsConfigAutomatic)

val finalExecution: ZIO[Console with Config[WsConfig], Nothing, Unit] =
  for {
    wsCommConfig <- config[WsConfig]
    _         <- putStrLn(" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ")
    _         <- putStrLn(s" DB ip = ${wsCommConfig.ip}")
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