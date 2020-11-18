package db

import java.sql.Connection
import zio.config.ZConfig
import env.EnvContainer.{ConfigWsConf, ZenvLogConfCache_}
import izumi.reflect.Tag
import wsconfiguration.ConfClasses.WsConfig
import zio.{Has, Ref, Task, UIO, ZIO, ZLayer, ZManaged}

object Ucp {

  type UcpZLayer = Has[UcpZLayer.Service]

  object UcpZLayer {

    trait Service {
      def getConnection: UIO[Connection]
      def setMaxPoolSize(newMaxPoolSize: Int): Task[Int]
      def getMaxPoolSize: UIO[Int]
      def setConnectionPoolName(name: String): UIO[Unit]
      def getConnectionPoolName: UIO[String]
      def getAvailableConnectionsCount: UIO[Int]
      def getBorrowedConnectionsCount: UIO[Int]
      def closeAll: UIO[Unit]
      def getJdbcVersion: UIO[String]
    }

    final class poolCache(ref: Ref[OraConnectionPool]) extends UcpZLayer.Service {
      override def getConnection: UIO[Connection] = ref.get.map(cp => cp.pds.getConnection())

      override def setMaxPoolSize(newMaxPoolSize: Int): Task[Int] = {
        ref.update(cp => {
          cp.pds.setMaxPoolSize(newMaxPoolSize)
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

      override def getAvailableConnectionsCount: UIO[Int] =
        ref.get.map(cp => cp.pds.getAvailableConnectionsCount)

      override def getBorrowedConnectionsCount: UIO[Int] =
        ref.get.map(cp => cp.pds.getBorrowedConnectionsCount)

      override def closeAll: UIO[Unit] =
        ref.get.map(_.closePoolConnections)

      override def getJdbcVersion: UIO[String] = ref.get.map(cp => cp.jdbcVersion)

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
      def poolCache(implicit tag: Tag[UcpZLayer.Service]): ZLayer[ZenvLogConfCache_, Throwable, UcpZLayer] = {
        val zm: ZManaged[ConfigWsConf, Throwable, poolCache] =
          for {
            // Use a Managed directly when access then environment
            conf <- ZManaged.access[ZConfig[WsConfig]](_.get)
            cp = new OraConnectionPool(conf.dbconf, conf.ucpconf)
            // Convert the effect into a no-release managed
            cpool <- Ref.make(cp).toManaged_
            // Create the managed
            zm <- ZManaged.make(ZIO(new poolCache(cpool)))(_.closeAll)
          } yield zm
        zm.toLayer // Convert a `Managed` to `ZLayer` directly
      }
  }

}