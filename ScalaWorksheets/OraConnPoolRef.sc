import java.sql.Connection
import java.sql.ResultSet

import oracle.ucp.jdbc.PoolDataSourceFactory
import zio._
import zio.ZLayer
import zio.console._
import oracle.ucp.jdbc.{PoolDataSourceFactory, ValidConnection}
import zio.blocking.blocking

final case class DbConfig(
                           ip: String,
                           port: Int = 1521,
                           sid: String,
                           username: String,
                           password: String
                         )

final case class ConnPoolProperties(
                                     ConnectionPoolName: String,
                                     InitialPoolSize: Int,
                                     MinPoolSize: Int,
                                     MaxPoolSize: Int
                                   )

/**
 * Universal Connection Pool Developer's Guide
 * https://docs.oracle.com/en/database/oracle/oracle-database/12.2/jjucp/toc.htm
 * Also read 4.3 About Optimizing Real-World Performance with Static Connection Pools
 */
class OraConnectionPool(conf: DbConfig, props: ConnPoolProperties){
  val pds = PoolDataSourceFactory.getPoolDataSource
  pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource")
  pds.setURL(s"jdbc:oracle:thin:@//${conf.ip}:${conf.port}/${conf.sid}")
  pds.setUser(conf.username)
  pds.setPassword(conf.password)
  pds.setConnectionPoolName(props.ConnectionPoolName)
  //4.4.4 Setting the Connection Wait Timeout
  pds.setConnectionWaitTimeout(30)
  pds.setInitialPoolSize(props.InitialPoolSize)
  pds.setMinPoolSize(props.MinPoolSize)
  pds.setMaxPoolSize(props.MaxPoolSize)


  /*
  (1 to props.MaxPoolSize).foreach{idx =>
    {val c = pds.getConnection()
      c.setClientInfo("OCSID.MODULE", "wsora_$idx");
      c.setClientInfo("OCSID.ACTION", "xyz");
    }
  }
  */

  def closePoolConnections: Unit = {
    val cnt: Int = pds.getAvailableConnectionsCount +
                   pds.getBorrowedConnectionsCount
    (1 to cnt)
    .foreach(_ => {
      val c = pds.getConnection()
      println("closing this connection")
      c.asInstanceOf[ValidConnection].setInvalid()
      c.close()
    }
    )
  }

}

//--------------------------------------------------------------------------------------------------------

object Listener {
  //#1
  type ListenAsZLayer = Has[ListenAsZLayer.Service]
  //#2
  object ListenAsZLayer {

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
    final class poolCache(ref: Ref[OraConnectionPool]) extends ListenAsZLayer.Service {
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
             "select sum(t.OBJECT_ID)as cnt from all_objects t where rownum<=round(dbms_random.value(1,100))"
             //"select id,s_name,view_src_data_name from d_data_source where id<=10"
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

    //#5
    def poolCache(implicit tag: Tagged[ListenAsZLayer.Service]
                ): ZLayer[Any, Nothing, ListenAsZLayer] = {
      ZLayer.fromEffect[Any,
        Nothing,
        ListenAsZLayer.Service
      ]{
        val dbconf = DbConfig("10.127.24.11", 1521, "test", "MSK_ARM_LEAD", "MSK_ARM_LEAD")
        val cpp = ConnPoolProperties("ChangeListener",3,3,3)
        Ref.make(new OraConnectionPool(dbconf,cpp)).map(cp => new poolCache(cp))
      }
    }

  }
}

import Listener._
import zio.duration._
object MyApp extends App {

  lazy val myenv: ZLayer[Any, Throwable, ZEnv with ListenAsZLayer] =
    ZEnv.live ++ ListenAsZLayer.poolCache

  val WsApp: List[String] => ZIO[ZEnv with ListenAsZLayer, Throwable, Unit] = args =>
    for {
      //c1 <- ZLayer.fromManaged(Managed.make(ZIO.access[ListenAsZLayer](_.get)))(sys => Task.fromFuture(_ => sys.terminate()).either))
      c <- ZIO.access[ListenAsZLayer](_.get)
      _ <- c.setMaxPoolSize(20)
      //_ <- putStrLn(s" resChange = $resChange")
      //without blocking rate of parallel = cpu cores  * 2
      _ <- blocking(ZIO.foreachPar(1 to 100){elm => c.testQuery(elm)})
      nb <- c.getConnectionPoolName
      _ <- putStrLn(s"BEFORE $nb")
      _ <- c.setConnectionPoolName("XXX")
      //_ <- ZIO.sleep(10.seconds)
      na <- c.getConnectionPoolName
      _ <- putStrLn(s"AFTER $na")
      currMaxPoolSize <- c.getMaxPoolSize
      _ <- putStrLn(s" getMaxPoolSize = $currMaxPoolSize")
     _ <- c.closeAll
    } yield ()

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    WsApp(args).provideCustomLayer(myenv).fold(_ => 1, _ => 0)

}

val runtime = Runtime.default
runtime.unsafeRun(MyApp.run(List()).provideLayer(MyApp.myenv))