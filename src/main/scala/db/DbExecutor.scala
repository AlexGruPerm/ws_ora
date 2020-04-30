package db

import java.sql.{CallableStatement, Connection, ResultSet, Types}
import java.util.concurrent.TimeUnit
import java.util.NoSuchElementException

import application.CacheLog
import data.RowType.rows
import data.{CacheEntity, DictDataRows, DictRow}
import db.Ucp.UcpZLayer
import env.CacheObject.CacheManager
import env.EnvContainer.ZEnvConfLogCache
import oracle.jdbc.{OracleCallableStatement, OracleTypes}
import reqdata.{Query, RequestHeader, func_cursor, proc, select}
import wsconfiguration.ConfClasses.{DbConfig, WsConfig}
import zio.logging.log
import zio.{Task, ZIO, clock}

object DbExecutor {

  type Notifications = Set[String]

  /**
   * Optimizationm scrollable r.s.
   * https://www.informit.com/articles/article.aspx?p=26251&seqNum=7
  */
  private def execFunctionCursor(conn: Connection, reqHeader: RequestHeader, query: Query) : rows ={
  //  val ucp = ZIO.access[UcpZLayer](_.get)//.map(ucp => ucp.getConnection)
  //  val conn :Connection = ucp.
    val stmt = conn.createStatement()
    //conn.setClientInfo("OCSID.ACTION", )

    //todo: remove into separate method.
    reqHeader.context match {
      case Some(ctx) => {
        val context = ctx.trim.replaceAll(" +", " ")
        println(s"SET CONTEXT ${context}")
        stmt.execute(context)
      }
      case None => ()
    }

    val call :CallableStatement = conn.prepareCall (s"{ ? = call ${query.query}}");
    call.registerOutParameter (1, OracleTypes.CURSOR);
    call.execute()

    val rs :ResultSet = call.getObject(1).asInstanceOf[ResultSet]

    val columns: List[(String, String)] = (1 to rs.getMetaData.getColumnCount)
      .map(cnum => (rs.getMetaData.getColumnName(cnum), rs.getMetaData.getColumnTypeName(cnum))).toList

    println(s"   columns.size : ${columns.size}")
    //columns.foreach(c => println(s"COLUMN = ${c._1} - ${c._2}" ))

    val rows = Iterator.continually(rs).takeWhile(_.next()).map { rs =>
      columns.map(
        cname => DictRow(cname._1, rs.getString(cname._1))
      )
    }.toList

    conn.close()
    rows
  }


  private def getCursorData(beginTs: Long, reqHeader: RequestHeader, query: Query, openConnDur: Long):
  ZIO[ZEnvConfLogCache, Throwable, DictDataRows] = for {
    _ <- log.info(s"getCursorData ${query.name} - ${query.qt.getClass.getTypeName}")

    ucp <- ZIO.access[UcpZLayer](_.get)
    conn <- ucp.getConnection

    rows <- query.qt match {
      case _: func_cursor.type => {
        log.info(">>>>>>>>>>>>> calling execFunctionCursor >>>>>>>>>>>>>>>") *>
        Task(execFunctionCursor(conn, reqHeader, query))
      }
      case _ =>
        Task(List(List(DictRow("xxx", "yyy"))))
    }

    ddr <- Task(DictDataRows(query.name, openConnDur, 123L, 234L, rows))
  } yield ddr

  /*
  private def getCursorData(beginTs: Long, /*conn: Connection,*/ reqHeader: RequestHeader, query: Query, openConnDur: Long):
  ZIO[ZEnvConfLogCache, Throwable, DictDataRows] = {
    log.info(s"getCursorData ${query.name} - ${query.qt.getClass.getTypeName}")

    query.qt match {
      case _: func_cursor.type => {
        val rows = execFunctionCursor(/*conn,*/ reqHeader, query)
        Task(DictDataRows(query.name, openConnDur, 123L, 234L, rows))
      }
      /*
      case _: proc.type => Task(execProcedure(conn,query))
      case _: select.type => Task(execSelect(conn,query))
      */
      case _ =>
        Task(DictDataRows(query.name,openConnDur, 0L, 0L, List(List(DictRow("xxx", "yyy"))) // rows
          )
        )
    }

  }
  */

  import zio.blocking._

  private def getDataFromDb: (RequestHeader,Query) => ZIO[ZEnvConfLogCache, Throwable, DictDataRows] =
    (reqHeader, reqQuery) =>
      for {
        cache <- ZIO.access[CacheManager](_.get)
        ucp <- ZIO.access[UcpZLayer](_.get)
        _ <- CacheLog.out("getDataFromDb", false)
        tBeforeOpenConn <- clock.currentTime(TimeUnit.MILLISECONDS)
        thisConnection <- ucp.getConnection //effectBlocking(pgPool.sess(thisConfig, trqDict)).refineToOrDie[PSQLException]
        tAfterOpenConn <- clock.currentTime(TimeUnit.MILLISECONDS)
        openConnDuration = tAfterOpenConn - tBeforeOpenConn
        dsCursor = getCursorData(tBeforeOpenConn, reqHeader, reqQuery, openConnDuration).refineToOrDie[java.sql.SQLException]
        /**
         * in getCursorData we can get error like this :
         * ║ java.sql.SQLException: ORA-06503: PL/SQL: Function return without value
         * ║ ORA-06512: на  "MSK_ARM_LEAD.PKG_ARM_DATA", line 2217
         * And we need produce result json with error description.
        */
        hashKey: Int = reqHeader.hashCode() + reqQuery.hashCode() //reqQuery.hashCode()  // todo: add user_session
        dictRows <- dsCursor
        _ <- cache.set(hashKey, CacheEntity(System.currentTimeMillis, System.currentTimeMillis, dictRows, reqQuery.reftables.getOrElse(Seq())))
        //ds <- dsCursor
        // We absolutely need close it to return to the pool
        _ = thisConnection.close()
        // If this connection was obtained from a pooled data source, then it won't actually be closed,
        // it'll just be returned to the pool.
      } yield dictRows


  val getDbResultSet: ( Query, RequestHeader) => ZIO[ZEnvConfLogCache, Throwable, DictDataRows] =
    ( trqDict, reqHeader) =>
      for {
        cache <- ZIO.access[CacheManager](_.get)
        //todo: remove this 2 outputs.
        //cv <- cache.getCacheValue
        //_ <- log.trace(s"getDict HeartbeatCounter = ${cv.HeartbeatCounter} ")
        valFromCache: Option[CacheEntity] <- cache.get(reqHeader.hashCode() + trqDict.hashCode()) //todo: ??!!
        dictRows <- valFromCache match {
          case Some(s: CacheEntity) =>
            log.trace(s"--- [VALUE GOT FROM CACHE] [${s.dictDataRows.name}] ---") *>
              ZIO.succeed(s.dictDataRows)
          case None => for {
            db <- getDataFromDb(reqHeader,trqDict)
            _ <- log.trace(s"--- [VALUE GOT FROM DB] [${db.name}] ---")
          } yield db
        }
      } yield dictRows

}
