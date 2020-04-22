package db

import java.sql.Connection

import Ucp.UcpZLayer
import db.Ucp.UcpZLayer.poolCache
import zio.config.Config
import env.EnvContainer.ZenvLogConfCache_
import wsconfiguration.ConfClasses.WsConfig
import zio.{Has, Ref, Tagged, Task, UIO, URIO, ZIO, ZLayer, ZManaged}

object Ucp {

  type UcpZLayer = Has[UcpZLayer.Service]

  object UcpZLayer {

    trait Service {
      def getConnection: UIO[Connection]

      def setMaxPoolSize(newMaxPoolSize: Int): Task[Int]

      def getMaxPoolSize: UIO[Int]

      def setConnectionPoolName(name: String): UIO[Unit]

      def getConnectionPoolName: UIO[String]

      def closeAll: UIO[Unit]

      def testQuery(i: Int): UIO[Unit]
    }

    final class poolCache(ref: Ref[OraConnectionPool]) extends UcpZLayer.Service {
      override def getConnection: UIO[Connection] = ref.get.map(cp => cp.pds.getConnection())

      override def setMaxPoolSize(newMaxPoolSize: Int): Task[Int] = {
        ref.update(cp => {
          cp.pds.setMaxPoolSize(newMaxPoolSize)
          cp.pds.setMinPoolSize(newMaxPoolSize)
          cp
        }
        ) *> getMaxPoolSize
      }

      /*
        override def getMaxPoolSize: ZIO[WsConfig, Nothing, Int] = ref.get.map(cp => cp.map(c => c.pds.getMaxPoolSize)).flatten
      */

      override def getMaxPoolSize: UIO[Int] =
        ref.get.map(cp => cp.pds.getMaxPoolSize)

      override def setConnectionPoolName(name: String): UIO[Unit] =
        ref.get.map(cp => cp.pds.setConnectionPoolName(name))

      override def getConnectionPoolName: UIO[String] =
        ref.get.map(cp => cp.pds.getConnectionPoolName)

      override def closeAll: UIO[Unit] =
        ref.get.map(_.closePoolConnections)

      override def testQuery(i: Int): UIO[Unit] = {
        ref.get.map(cp => cp.pds.getConnection()).map { con =>
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
          rows.foreach(r => println("[" + i + "]    " + r))
        }
      }


    }

    /**
     * If you need finalization use ZLayer.fromManaged and put your finalizer in the release action of the Managed.
     * We use it to close all connections in pool if exception is raising.
     * java.sql.SQLException: Исключение при получении соединения:
     * oracle.ucp.UniversalConnectionPoolException: Все соединения из универсального пула соединений заняты
     * We need adjust pool to eliminate this cases, use MaxPoolSize and ConnectionWaitTimeout(seconds)
     */
    /**
     * No, I don't think you want to have a ZIO inside a Ref.
     * if you have your value inside an effect you can just do effect.map(ref.set)
     */

    def poolCache(implicit tag: Tagged[UcpZLayer.Service]): ZLayer[ZenvLogConfCache_, Throwable, UcpZLayer] = {

      val zm: ZIO[WsConfig, Throwable, ZManaged[Any, Throwable, UcpZLayer.Service]] =
        for {
          conf <- ZIO.environment[WsConfig]
          cpool <- Ref.make(new OraConnectionPool(conf.dbconf, conf.ucpconf))
          ucpL = ZIO(new poolCache(cpool))
          release: (poolCache => zio.ZIO[Any,Nothing,Any]) = (p: poolCache) => p.closeAll
          zm: ZManaged[Any, Throwable, UcpZLayer.Service] = ZManaged.make(ucpL)(release)
        } yield zm

      val managedConnPool: ZManaged[Any, Throwable, UcpZLayer.Service] = ???

      ZLayer.fromManaged(managedConnPool)
    }
/*
Error:(106, 81) type mismatch;
 found   : zio.ZIO[poolCache,Throwable,Unit]
 required: poolCache => zio.ZIO[Any,Nothing,Any]
          zm: ZManaged[Any, Throwable, UcpZLayer.Service] = ZManaged.make(ucpL)(release)
    */

      /*
original
  val mr: ZManaged[Any, Nothing, UcpZLayer.Service] =
    ZManaged.make(
      Ref.make(
        ZIO.access[Config[WsConfig]](_.get).map(cfg => new OraConnectionPool(cfg.dbconf, cfg.ucpconf))
      ).map(cp => new poolCache(Task(cp)))
    )(_.closeAll)
*/

  }

}