package db

import java.sql.{CallableStatement, Connection, ResultSet, Types}
import java.util.concurrent.TimeUnit
import java.util.NoSuchElementException

import application.CacheLog
import data.RowType.{rows}
import data.{CacheEntity, DbErrorException, DictDataRows, DictRow, Notification}
import db.Ucp.UcpZLayer
import env.CacheObject.CacheManager
import env.EnvContainer.ZEnvConfLogCache
import oracle.jdbc.{OracleCallableStatement, OracleTypes}
import reqdata.{Query, RequestHeader, func_cursor, func_simple, proc_cursor, select}
import wsconfiguration.ConfClasses.{DbConfig, WsConfig}
import zio.logging.log
import zio.{Task, ZIO, clock}

object DbExecutor {

  type Notifications = List[String]

  private def execGlobalCursor(conn: Connection, reqHeader: RequestHeader) :Unit ={
    reqHeader.context match {
      case Some(ctx) => {
        val context = ctx.trim.replaceAll(" +", " ")
        println(s"SET CONTEXT ${context}")
        conn.createStatement.execute(context)
      }
      case None => ()
    }
  }

  private def getChangedTables(conn: Connection) : Notifications = {
    conn.setClientInfo("OCSID.MODULE", "WS_NOTIF")
    conn.setClientInfo("OCSID.ACTION", "NOTIF_QUERY")
    val call :CallableStatement = conn.prepareCall(s"begin wsora.get_notifications(?); end;")
    call.registerOutParameter (1, OracleTypes.CURSOR);
    call.execute()
    val rs :ResultSet = call.getObject(1).asInstanceOf[ResultSet]
    val columns: List[(String, String)] = (1 to rs.getMetaData.getColumnCount)
      .map(cnum => (rs.getMetaData.getColumnName(cnum), rs.getMetaData.getColumnTypeName(cnum))).toList
    val rows = Iterator.continually(rs).takeWhile(_.next()).map { rs =>
      columns.map(
        cname => rs.getString(cname._1)
      )
    }.toList
    conn.close()
    rows.flatten
  }

  private def getRowsFromResultSet(rs: ResultSet): rows ={
    val columns: IndexedSeq[(String, String)] = (1 to rs.getMetaData.getColumnCount)
      .map(cnum => (rs.getMetaData.getColumnName(cnum), rs.getMetaData.getColumnTypeName(cnum)))
    Iterator.continually(rs).takeWhile(_.next()).map { rs =>
      columns.map(
        cname => DictRow(cname._1, rs.getString(cname._1))
      )
    }.toList
  }

  /**
   * Optimizationm scrollable r.s.
   * https://www.informit.com/articles/article.aspx?p=26251&seqNum=7
  */
  private def execFunctionCursor(conn: Connection, reqHeader: RequestHeader, query: Query) : rows = {
    conn.setClientInfo("OCSID.MODULE", "WS_CONN")
    conn.setClientInfo("OCSID.ACTION", "FUNC_CURSOR")
    execGlobalCursor(conn, reqHeader)
    val call :CallableStatement = conn.prepareCall (s"{ ? = call ${query.query}}")
    call.registerOutParameter (1, OracleTypes.CURSOR);
    println("execFunctionCursor  BEFORE call.execute")//todo: remove it
    call.execute()
    println("execFunctionCursor  AFTER  call.execute")//todo: remove it
    val rs :ResultSet = call.getObject(1).asInstanceOf[ResultSet]
    val rows = getRowsFromResultSet(rs)
    conn.close()
    rows
  }

  private def execFunctionSimple(conn: Connection, reqHeader: RequestHeader, query: Query) : rows = {
    conn.setClientInfo("OCSID.MODULE", "WS_CONN")
    conn.setClientInfo("OCSID.ACTION", "FUNC_SIMPLE")
    execGlobalCursor(conn, reqHeader)
    val rs :ResultSet = conn.createStatement.executeQuery(s"select ${query.query} as result from dual")
    val rows = getRowsFromResultSet(rs)
    conn.close()
    rows
  }

  private def execSimpleQuery(conn: Connection, reqHeader: RequestHeader, query: Query) : rows = {
    conn.setClientInfo("OCSID.MODULE", "WS_CONN")
    conn.setClientInfo("OCSID.ACTION", "SELECT")
    execGlobalCursor(conn, reqHeader)
    val rs :ResultSet = conn.createStatement.executeQuery(s"${query.query}")
    val rows = getRowsFromResultSet(rs)
    conn.close()
    rows
  }

  private def execProcCursor(conn: Connection, reqHeader: RequestHeader, query: Query) : rows = {
    conn.setClientInfo("OCSID.MODULE", "WS_CONN")
    conn.setClientInfo("OCSID.ACTION", "PROC_CURSOR")
    execGlobalCursor(conn, reqHeader)
    val call :CallableStatement = conn.prepareCall(s"begin ${query.query}; end;")
    call.registerOutParameter (1, OracleTypes.CURSOR);
    call.execute()
    val rs :ResultSet = call.getObject(1).asInstanceOf[ResultSet]
    val rows = getRowsFromResultSet(rs)
    conn.close()
    rows
  }

  private def getDictData(beginTs: Long, reqHeader: RequestHeader, query: Query, openConnDur: Long):
  ZIO[ZEnvConfLogCache, Throwable, DictDataRows] = for {
    _ <- log.info(s"getDictData ${query.name} - ${query.qt.getClass.getTypeName}")
    ucp <- ZIO.access[UcpZLayer](_.get)
    conn <- ucp.getConnection
    /**
     *  func_simple
     *  func_cursor
     *  proc_cursor
     *  select
    */
    rows <- (query.qt match {
      case _ : func_simple.type  => {
        log.info(">>>>>>>>>>>>> calling execFunctionSimple >>>>>>>>>>>>>>>") *>
          Task(execFunctionSimple(conn, reqHeader, query))
      }
      case _ : func_cursor.type => {
        log.info(">>>>>>>>>>>>> calling execFunctionCursor >>>>>>>>>>>>>>>") *>
        Task(execFunctionCursor(conn, reqHeader, query))
      }
      case _ : proc_cursor.type => {
        log.info(">>>>>>>>>>>>> calling execProcCursor >>>>>>>>>>>>>>>") *>
          Task(execProcCursor(conn, reqHeader, query))
      }
      case _ : select.type  => {
        log.info(">>>>>>>>>>>>> calling execSimpleQuery >>>>>>>>>>>>>>>") *>
          Task(execSimpleQuery(conn, reqHeader, query))
      }
      case _ => Task(List[Seq[data.DictRow]]())
    }).catchSome{
      case se: java.sql.SQLException =>
        Task(conn.close()) *>
        log.error(s"${query.qt} SQLException Code: ${se.getErrorCode} ${se.getLocalizedMessage} ") *>
          Task.fail(new java.sql.SQLException(se))
    }

    _ <- log.error("before ddr ----------------------------------------")
    ddr <- Task(DictDataRows(query.name, openConnDur, 123L, 234L, rows))
    //todo: close connection here like in next method !!!!!!!!!
    //13.11.2020
    _ <- log.error("after ddr ----------------------------------------")
    _ = conn.close()
    //
  } yield ddr



  import zio.blocking._

  private def getDataFromDb: (RequestHeader,Query) => ZIO[ZEnvConfLogCache, Throwable, DictDataRows] =
    (reqHeader, reqQuery) =>
      for {
        cache <- ZIO.access[CacheManager](_.get)
        _ <- CacheLog.out("getDataFromDb", false)
        tBeforeOpenConn <- clock.currentTime(TimeUnit.MILLISECONDS)
        dsCursor = getDictData(tBeforeOpenConn, reqHeader, reqQuery, 0L).refineToOrDie[java.sql.SQLException]
        /**
         * in getDictData we can get error like this :
         * ║ java.sql.SQLException: ORA-06503: PL/SQL: Function return without value
         * ║ ORA-06512: на  "MSK_ARM_LEAD.PKG_ARM_DATA", line 2217
         * And we need produce result json with error description.
        */
        hashKey: Int = reqHeader.hashCode() + reqQuery.hashCode()  // todo: add user_session
        dictRows <- dsCursor
        currTs <- clock.currentTime(TimeUnit.MILLISECONDS)
        //We cache results only if nocache key is empty or = 0
        _ <- cache.set(hashKey,
          CacheEntity(currTs, currTs,
            dictRows, reqQuery.reftables.getOrElse(Seq())))
          .when(reqHeader.nocache.forall(_ == 0) &&
            reqQuery.nocache.forall(_ == 0))

        // We absolutely need close it to return to the pool
        //_ = thisConnection.close()
        // If this connection was obtained from a pooled data source, then it won't actually be closed,
        // it'll just be returned to the pool.
      } yield dictRows


  val getDbResultSet: ( Query, RequestHeader) => ZIO[ZEnvConfLogCache, Throwable, DictDataRows] =
    ( trqDict, reqHeader) =>
      for {
        cache <- ZIO.access[CacheManager](_.get)
        //todo: remove this 2 outputs.
        valFromCache: Option[CacheEntity] <- cache.get(reqHeader.hashCode() + trqDict.hashCode()) //todo: ??!!
        dictRows <- valFromCache match {
          case Some(s: CacheEntity) =>
            log.trace(s"--- [VALUE GOT FROM CACHE] [${s.dictDataRows.name}] ---") *>
              ZIO.succeed(s.dictDataRows)
          case None => (for {
            db <- getDataFromDb(reqHeader,trqDict)
            _ <- log.trace(s"--- [VALUE GOT FROM DB] [${db.name}] ---")
          } yield db)
           .catchSome{
            case err: java.sql.SQLException =>
              ZIO.fail(DbErrorException(err.getMessage, err.getCause, trqDict.name))
              // todo #2: here we can extend DbErrorException with ORA- error code, stack trace an etc.
              //ZIO.fail(DbErrorException(err.getMessage, err.getCause, trqDict.name+" - ErrrorCode : "+err.getErrorCode+" - "+err.getSQLState))
          }

        }
      } yield dictRows

  val getNotifications: ZIO[ZEnvConfLogCache, Throwable, Notifications] = for{
    ucp <- ZIO.access[UcpZLayer](_.get)
    conn <- ucp.getConnection
    //_ <- log.trace("getNotifications")
    nts <- Task(getChangedTables(conn))
    _ = conn.close()
  } yield nts

}
