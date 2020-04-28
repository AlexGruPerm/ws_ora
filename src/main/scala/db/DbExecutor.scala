package db

import java.sql.{Connection, Types}
import java.util.concurrent.TimeUnit
import java.util.NoSuchElementException

import application.CacheLog
import data.{CacheEntity, DictDataRows, DictRow}
import db.Ucp.UcpZLayer
import env.CacheObject.CacheManager
import env.EnvContainer.ZEnvConfLogCache
import reqdata.Query
import wsconfiguration.ConfClasses.{DbConfig, WsConfig}
import zio.logging.log
import zio.{Task, ZIO, clock}

object DbExecutor {

  type Notifications = Set[String]

  private def getCursorData(beginTs: Long, conn: Connection, query: Query, openConnDur: Long):
  ZIO[ZEnvConfLogCache,Throwable,DictDataRows]/*Task[DictDataRows]*/ =
  {
/*
    val stmt = conn.sess.prepareCall(s"{call ${query.proc} }")
    stmt.setNull(1, Types.OTHER)
    stmt.registerOutParameter(1, Types.OTHER)
    stmt.execute()
    val afterExecTs: Long = System.currentTimeMillis
    // org.postgresql.jdbc.PgResultSet
    //val refCur = stmt.getObject(1)
    val pgrs: PgResultSet = stmt.getObject(1).asInstanceOf[PgResultSet]
    val columns: List[(String, String)] = (1 to pgrs.getMetaData.getColumnCount)
      .map(cnum => (pgrs.getMetaData.getColumnName(cnum), pgrs.getMetaData.getColumnTypeName(cnum))).toList
    // here we itarate over all rows (PgResultSet) and for each row iterate over our columns,
    // and extract cells data with getString.
    //List[List[DictRow]]
    val rows = Iterator.continually(pgrs).takeWhile(_.next()).map { rs =>
      columns.map(cname => DictRow(cname._1, rs.getString(cname._1)))
    }.toList
*/

    Task(
      DictDataRows(
        query.name,
        openConnDur,
        0L,//afterExecTs - beginTs,
        0L,//ystem.currentTimeMillis - afterExecTs,
        List(List(DictRow("xxx", "yyy")))// rows
      )
    )
  }

  import zio.blocking._

  private def getDataFromDb: Query => ZIO[ZEnvConfLogCache, Throwable, DictDataRows] = reqQuery =>
      for {
        cache <- ZIO.access[CacheManager](_.get)
        thisConfig = ZIO.access[DbConfig]
        conn <- ZIO.access[UcpZLayer](_.get)
        _ <- CacheLog.out("getDataFromDb", false)
        /*
        thisConfig <-
          if (configuredDb.name == trqDict.db) {
            Task(configuredDb)
          }
          else {
            IO.fail(new NoSuchElementException(s"Database name [${trqDict.db}] not found in config."))
          }
        */
        tBeforeOpenConn <- clock.currentTime(TimeUnit.MILLISECONDS)
        thisConnection <- conn.getConnection//effectBlocking(pgPool.sess(thisConfig, trqDict)).refineToOrDie[PSQLException]
        tAfterOpenConn <- clock.currentTime(TimeUnit.MILLISECONDS)
        openConnDuration = tAfterOpenConn - tBeforeOpenConn
        dsCursor = getCursorData(tBeforeOpenConn, thisConnection, reqQuery, openConnDuration)
        hashKey: Int = reqQuery.hashCode() //todo: add user_session
        dictRows <- dsCursor
        _ <- cache.set(hashKey, CacheEntity(System.currentTimeMillis, dictRows, reqQuery.reftables.getOrElse(Seq())))
        ds <- dsCursor
        // We absolutely need close it to return to the pool
        _ = thisConnection.close()
        // If this connection was obtained from a pooled data source, then it won't actually be closed,
        // it'll just be returned to the pool.
      } yield ds


  val getDbResultSet: (DbConfig, Query) => ZIO[ZEnvConfLogCache, Throwable, DictDataRows] =
    (configuredDb, trqDict) =>
      for {
        cache <- ZIO.access[CacheManager](_.get)
        //todo: remove this 2 outputs.
        //cv <- cache.getCacheValue
        //_ <- log.trace(s"getDict HeartbeatCounter = ${cv.HeartbeatCounter} ")
        valFromCache: Option[CacheEntity] <- cache.get(trqDict.hashCode())
        dictRows <- valFromCache match {
          case Some(s: CacheEntity) =>
            log.trace(s"--- [VALUE GOT FROM CACHE] [${s.dictDataRows.name}] ---") *>
              ZIO.succeed(s.dictDataRows)
          case None => for {
            db <- getDataFromDb(trqDict)
            _ <- log.trace(s"--- [VALUE GOT FROM DB] [${db.name}] ---")
          } yield db
        }
      } yield dictRows

}
