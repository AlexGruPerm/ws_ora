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

type argLayer = Has[String]

val wsConfigAutomatic = descriptor[WsConfig]

val configLayer: ZLayer[argLayer, Throwable, config.Config[WsConfig]] = {
  val confLayerRes :ZLayer[argLayer, Throwable, config.Config[WsConfig]]
  = TypesafeConfig.fromHoconFile(new java.io.File("C:\\ws_ora\\src\\main\\resources\\application.conf"), wsConfigAutomatic)
  confLayerRes
}

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
    //send arg into argLayer here, or build argLayer here with args.
    val args_head: String = "C:\\ws_ora\\src\\main\\resources\\application.conf"
    val argLayer: Layer[Nothing,Has[String]] = ZLayer.succeed(args_head)

    val appLayer = ZEnv.live ++ (argLayer >>> configLayer)
    val prg = for {
      _  <- finalExecution.provideCustomLayer(appLayer)
    } yield ()
    prg.fold(_ => 1, _ => 0)
  }
}

val runtime = Runtime.default
runtime.unsafeRun(MyApp.run(List()))