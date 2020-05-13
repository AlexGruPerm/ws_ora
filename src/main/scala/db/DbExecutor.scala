package db

import java.sql.{CallableStatement, Connection, ResultSet, Types}
import java.util.concurrent.TimeUnit
import java.util.NoSuchElementException

import application.CacheLog
import data.RowType.rows
import data.{CacheEntity, DbErrorException, DictDataRows, DictRow}
import db.Ucp.UcpZLayer
import env.CacheObject.CacheManager
import env.EnvContainer.ZEnvConfLogCache
import oracle.jdbc.{OracleCallableStatement, OracleTypes}
import reqdata.{Query, RequestHeader, func_cursor, func_simple, proc_cursor, select}
import wsconfiguration.ConfClasses.{DbConfig, WsConfig}
import zio.logging.log
import zio.{Task, ZIO, clock}

object DbExecutor {

  type Notifications = Set[String]

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

  /**
   * Optimizationm scrollable r.s.
   * https://www.informit.com/articles/article.aspx?p=26251&seqNum=7
  */
  private def execFunctionCursor(conn: Connection, reqHeader: RequestHeader, query: Query) : rows = {
    //conn.setClientInfo("OCSID.ACTION", )
    execGlobalCursor(conn, reqHeader)

    val call :CallableStatement = conn.prepareCall (s"{ ? = call ${query.query}}")
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

  private def execFunctionSimple(conn: Connection, reqHeader: RequestHeader, query: Query) : rows = {
    //conn.setClientInfo("OCSID.ACTION", )
    execGlobalCursor(conn, reqHeader)

    val rs :ResultSet = conn.createStatement.executeQuery(s"select ${query.query} as result from dual")

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

  private def execSimpleQuery(conn: Connection, reqHeader: RequestHeader, query: Query) : rows = {
    //conn.setClientInfo("OCSID.ACTION", )
    execGlobalCursor(conn, reqHeader)

    val rs :ResultSet = conn.createStatement.executeQuery(s"${query.query}")

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

  private def execProcCursor(conn: Connection, reqHeader: RequestHeader, query: Query) : rows = {
    //conn.setClientInfo("OCSID.ACTION", )
    execGlobalCursor(conn, reqHeader)

    //val call :CallableStatement = conn.prepareCall(s"begin ${query.query}(?); end;")

    val call :CallableStatement = conn.prepareCall(s"begin ${query.query}; end;")
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
    rows <- query.qt match {
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
      case _ => Task(List(List(DictRow("xxx", "yyy"))))
    }

    ddr <- Task(DictDataRows(query.name, openConnDur, 123L, 234L, rows))
  } yield ddr



  import zio.blocking._

  private def getDataFromDb: (RequestHeader,Query) => ZIO[ZEnvConfLogCache, Throwable, DictDataRows] =
    (reqHeader, reqQuery) =>
      for {
        cache <- ZIO.access[CacheManager](_.get)
        ucp <- ZIO.access[UcpZLayer](_.get)
        _ <- CacheLog.out("getDataFromDb", false)
        tBeforeOpenConn <- clock.currentTime(TimeUnit.MILLISECONDS)
        thisConnection <- ucp.getConnection
        tAfterOpenConn <- clock.currentTime(TimeUnit.MILLISECONDS)
        openConnDuration = tAfterOpenConn - tBeforeOpenConn
        dsCursor = getDictData(tBeforeOpenConn, reqHeader, reqQuery, openConnDuration).refineToOrDie[java.sql.SQLException]
        /**
         * in getDictData we can get error like this :
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
          case None => (for {
            db <- getDataFromDb(reqHeader,trqDict)
            _ <- log.trace(s"--- [VALUE GOT FROM DB] [${db.name}] ---")
          } yield db).catchSome{
            case err: java.sql.SQLException =>
              ZIO.fail(DbErrorException(err.getMessage, err.getCause, trqDict.name))
              // todo #2: here we can extend DbErrorException with ORA- error code, stack trace an etc.
              //ZIO.fail(DbErrorException(err.getMessage, err.getCause, trqDict.name+" - ErrrorCode : "+err.getErrorCode+" - "+err.getSQLState))
          }
        }
      } yield dictRows

}
