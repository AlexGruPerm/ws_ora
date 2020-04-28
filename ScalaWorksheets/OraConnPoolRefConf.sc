import java.io.File
import java.sql.Connection
import java.sql.ResultSet

import oracle.ucp.jdbc.PoolDataSourceFactory
import zio._
import zio.ZLayer
import zio.console._
import oracle.ucp.jdbc.{PoolDataSourceFactory, ValidConnection}
import zio.blocking.blocking
import zio.clock.Clock
import zio.logging.Logging
import zio.logging.Logging.Logging

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

final case class Config(api: ApiConfig, dbConfig: DbConfig, ucpcfg: UcpConfig)


object ObjectConfZLayer {
  type Configuration = zio.Has[Configuration.Service]
  object Configuration {
    trait Service {
      //val load: String => Task[Config]
      val tConf :Task[Config]
    }

    /*
    trait wsConf extends Configuration.Service {
      override val load: String => Task[Config] = confFileName =>
        Task.effect(ConfigSource.file(new File(confFileName)).loadOrThrow[Config])
    }
    */
    final class wsConf(fn: String) extends Configuration.Service {
      //override val load: String => Task[Config] = confFileName =>
      //  Task.effect(ConfigSource.file(new File(confFileName)).loadOrThrow[Config])
      override val tConf :Task[Config] = Task.effect(ConfigSource.file(new File(fn)).loadOrThrow[Config])
    }

    def wsConf(implicit tag: Tagged[Configuration.Service]): ZLayer[String, Nothing, Configuration] = {
      ZLayer.succeed(new wsConf("C:\\ws_ora\\src\\main\\resources\\application.conf"))
    }
  }
}

/**
 * Universal Connection Pool Developer's Guide
 * https://docs.oracle.com/en/database/oracle/oracle-database/12.2/jjucp/toc.htm
 * Also read 4.3 About Optimizing Real-World Performance with Static Connection Pools
 */
class OraConnectionPool(conf: DbConfig, props: UcpConfig){
  val pds = PoolDataSourceFactory.getPoolDataSource
  pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource")
  pds.setURL(s"jdbc:oracle:thin:@//${conf.ip}:${conf.port}/${conf.sid}")
  pds.setUser(conf.username)
  pds.setPassword(conf.password)
  pds.setConnectionPoolName(props.ConnectionPoolName)
  pds.setConnectionWaitTimeout(props.ConnectionWaitTimeout)
  pds.setInitialPoolSize(props.InitialPoolSize)
  // A connection pool always tries to return to the minimum pool size
  pds.setMinPoolSize(props.MinPoolSize)
  //The maximum pool size property specifies the maximum number of available
  // and borrowed (in use) connections that a pool maintains.
  pds.setMaxPoolSize(props.MaxPoolSize)

  def closePoolConnections: Unit = (1 to pds.getAvailableConnectionsCount + pds.getBorrowedConnectionsCount)
    .foreach(_ => {
      val c = pds.getConnection()
      println("closing this connection")
      c.asInstanceOf[ValidConnection].setInvalid()
      c.close()
    }
    )

}

import ObjectConfZLayer._
//--------------------------------------------------------------------------------------------------------


//Universal Connection Pool
object Ucp {
  //#1
  type UcpZLayer = Has[UcpZLayer.Service]
  //#2
  object UcpZLayer {

    //#3
    trait Service {
      def getConnection: UIO[Connection]
      def setMaxPoolSize(newMaxPoolSize: Int): Task[Int]
      def getMaxPoolSize: UIO[Int]
      def setConnectionPoolName(name: String): UIO[Unit]
      def getConnectionPoolName: UIO[String]
      def closeAll: UIO[Unit]
      def testQuery(i: Int): UIO[Unit]
    }

    //#4
    final class poolCache(ref: Ref[OraConnectionPool]) extends UcpZLayer.Service {
      override def getConnection: UIO[Connection] = ref.get.map(cp => cp.pds.getConnection())

      override def setMaxPoolSize(newMaxPoolSize: Int): Task[Int] = {
        ref.update(cp => {cp.pds.setMaxPoolSize(newMaxPoolSize)
          cp.pds.setMinPoolSize(newMaxPoolSize)
          cp
        }
        ) *> getMaxPoolSize
      }

      override def getMaxPoolSize: UIO[Int] =
        ref.get.map(cp => cp.pds.getMaxPoolSize)

      override def setConnectionPoolName(name: String): UIO[Unit] =
        ref.get.map(cp => cp.pds.setConnectionPoolName(name))

      override def getConnectionPoolName: UIO[String] =
        ref.get.map(cp => cp.pds.getConnectionPoolName)

      override def closeAll: UIO[Unit] =
        ref.get.map(_.closePoolConnections)

      override def testQuery(i: Int): UIO[Unit] = {
         ref.get.map(cp => cp.pds.getConnection()).map{ con =>
           val stmt = con.createStatement()
           con.setClientInfo("OCSID.ACTION", i.toString)
           val rs = stmt.executeQuery(
             "select * from msk_arm_lead.econom_data_source"
           )
           con.close() //!!!
           val columns: List[(String, String)] = (1 to rs.getMetaData.getColumnCount)
             .map(cnum => (rs.getMetaData.getColumnName(cnum), rs.getMetaData.getColumnTypeName(cnum))).toList
           //columns.foreach(c => println(s"${c._1} - ${c._2}" ))
           val rows = Iterator.continually(rs).takeWhile(_.next()).map { rs =>
             columns.map(
               cname => (cname._1, rs.getString(cname._1))
             )
           }.toList
           rows.foreach(r => println("["+i+"]    "+r))
         }
      }


    }

    /**
     * If you need finalization use ZLayer.fromManaged and put your finalizer in the release action of the Managed.
     * We use it to close all connections in pool if exception raising.
     * java.sql.SQLException: Исключение при получении соединения:
     * oracle.ucp.UniversalConnectionPoolException: Все соединения из универсального пула соединений заняты
     * We need adjust pool to eliminate this cases, use MaxPoolSize and ConnectionWaitTimeout(seconds)
    */
    def poolCache(implicit tag: Tagged[UcpZLayer.Service]): ZLayer[Configuration, Throwable, UcpZLayer] = {
        /*
        cfg <-  ZIO.access[Configuration](_.get.load("C:\\ws_ora\\src\\main\\resources\\application.conf"))
        dbconf <- cfg.map(_.dbConfig)
        cpp <- cfg.map(_.ucpcfg)
        */

      val dbconf = DbConfig("10.127.24.11", 1521, "test", "MSK_ARM_LEAD", "MSK_ARM_LEAD")

      val cpp = UcpConfig(
        ConnectionPoolName = "ChangeListener",
        InitialPoolSize = 1,
        MinPoolSize = 3,
        MaxPoolSize = 10,
        ConnectionWaitTimeout = 10
      )

      val mr: ZManaged[Any, Nothing, UcpZLayer.Service] =
        ZManaged.make(
          Ref.make(new OraConnectionPool(dbconf, cpp)).map(cp => new poolCache(cp))
        )(_.closeAll)
      ZLayer.fromManaged(mr)
    }


  }
}
//--------------------------------------------------------------------------------------------------------
import ObjectConfZLayer._
import Ucp._

object env {
  type ZEnvLogConfCache =  ZEnv with Logging with Configuration with UcpZLayer

  val env: ZLayer[Console with Clock, Nothing, Logging]   =
    Logging.console((_, logEntry) =>
      logEntry
    )

  val agrLayer :ZLayer[Any,Nothing,String] = ZLayer.succeed(
    new Service {
      def getUser(userId: UserId): IO[DBError, Option[User]] = UIO(???)
    }
  )

  val confZLayer :ZLayer[String,Nothing,Configuration] = agrLayer >>> Configuration.wsConf

  val WsEnv: ZLayer[ZEnv, Throwable, ZEnvLogConfCache] =
    ZEnv.live ++ env ++ confZLayer ++ (confZLayer >>> UcpZLayer.poolCache)
}



import Ucp._
import zio.duration._
object MyApp extends App {

  lazy val myenv: ZLayer[ZEnv, Throwable, env.ZEnvLogConfCache] = env.WsEnv

  val WsApp: List[String] => ZIO[env.ZEnvLogConfCache, Throwable, Unit] = args =>
    for {
      cfg <- ZIO.access[Configuration](_.get)
      c <- ZIO.access[UcpZLayer](_.get)
      _ <- c.setMaxPoolSize(5)
      //_ <- putStrLn(s" resChange = $resChange")
      //without blocking rate of parallel = cpu cores  * 2
      _ <- blocking(ZIO.foreachPar(1 to 50){elm => c.testQuery(elm)})
      nb <- c.getConnectionPoolName
      _ <- putStrLn(s"BEFORE $nb")
      _ <- c.setConnectionPoolName("XXX")
      //_ <- ZIO.sleep(10.seconds)
      na <- c.getConnectionPoolName
      _ <- putStrLn(s"AFTER $na")
      currMaxPoolSize <- c.getMaxPoolSize
      _ <- putStrLn(s" getMaxPoolSize = $currMaxPoolSize")
    // _ <- c.closeAll
    } yield ()

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    WsApp(args).provideCustomLayer(myenv).fold(_ => 1, _ => 0)

}

val runtime = Runtime.default
runtime.unsafeRun(MyApp.run(List()).provideLayer(MyApp.myenv))