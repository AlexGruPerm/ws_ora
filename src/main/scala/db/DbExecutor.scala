package db

import java.sql.{CallableStatement, Connection, ResultSet}
import java.util.concurrent.TimeUnit

import application.CacheLog
import data.RowType.rows
import data.{CacheEntity, CellType, DataCell, DbErrorException, DictDataRows, IntType, NumType, StrType}
import db.Ucp.UcpZLayer
import env.CacheObject.CacheManager
import env.EnvContainer.ZEnvConfLogCache
import oracle.jdbc.OracleTypes
import reqdata.{Query, RequestHeader, convType, func_cursor, func_simple, num, proc_cursor, select, str}
import zio.clock.Clock
import zio.logging.log
import zio.{Task, ZIO, clock}

import scala.collection.immutable.ListMap

object DbExecutor {

  type Notifications = List[String]

  private def execGlobalCursor(conn: Connection, reqHeader: RequestHeader): Unit = {
    reqHeader.context match {
      case Some(ctx) => {
        val context = ctx.trim.replaceAll(" +", " ")
        conn.setClientInfo("OCSID.MODULE", "WS_CONN")
        conn.setClientInfo("OCSID.ACTION", "SET_CONTEXT")
        conn.createStatement.execute(context)
      }
      case None => ()
    }
  }

  private def getChangedTables(conn: Connection): Notifications = {
    conn.setClientInfo("OCSID.MODULE", "WS_NOTIF")
    conn.setClientInfo("OCSID.ACTION", "NOTIF_QUERY")
    val call: CallableStatement = conn.prepareCall(s"begin wsora.get_notifications(?); end;")
    call.registerOutParameter(1, OracleTypes.CURSOR);
    call.execute()
    val rs: ResultSet = call.getObject(1).asInstanceOf[ResultSet]
    val columns: List[(String, String)] = (1 to rs.getMetaData.getColumnCount)
      .map(cnum => (rs.getMetaData.getColumnName(cnum), rs.getMetaData.getColumnTypeName(cnum))).toList
    val rows = Iterator.continually(rs).takeWhile(_.next()).map { rs =>
      columns.map(
        cname => rs.getString(cname._1)
      )
    }.toList
    rows.flatten
  }

  private def seqCellToMap(sc: IndexedSeq[DataCell]): ListMap[String, Option[CellType]] =
    sc.foldLeft(ListMap.empty[String, Option[CellType]]) {
      case (acc, pr) => acc + (pr.name -> pr.value)
    }

  /**
   * todo: add here using new control parameter from request,
   * smth. like this:
   * convType : str(default), num
   */

  private def getColumnWsDatatype(ColumnTypeName: String, Precision: Int, Scale: Int, ct: convType): String = {
    ct match {
      case _: num.type => if (ColumnTypeName == "NUMBER") {
                            if (Precision > 0 && Scale == 0) "INTEGER"
                            else "DOUBLE"
                          } else "STRING"
      case _: str.type => "STRING"
    }
  }

  private def isNumInString(s: String) = {
    if (s!=null && s.nonEmpty)
      s.replace(".", "").replace(",", "").replace("-","")
        .forall(_.isDigit)
    else
      false
  }


  private def getRowsFromResultSet(rs: ResultSet, ct: convType): rows ={
    val columns: IndexedSeq[(String, String)] = (1 to rs.getMetaData.getColumnCount)
      .map(cnum => (rs.getMetaData.getColumnName(cnum),
        getColumnWsDatatype(
          rs.getMetaData.getColumnTypeName(cnum),
          rs.getMetaData.getPrecision(cnum),
          rs.getMetaData.getScale(cnum),
          ct
        )))

    Iterator.continually(rs).takeWhile(_.next()).map { rs =>
      columns.map {
        cname =>
          DataCell(
            cname._1.toLowerCase,
            {
              val cellValue: CellType =
                cname._2 match {
                  case "INTEGER" => IntType(rs.getInt(cname._1))
                  case "DOUBLE" => {
                    val d: Double = rs.getDouble(cname._1)
                    if (d%1 == 0.0)
                      IntType(d.toInt)
                    else
                      NumType(d)
                  }
                  case _ => {
                    val s: String = rs.getString(cname._1)
                    ct match {
                      case _: num.type =>
                          if (isNumInString(s)){
                            val d: Double = s.replace(",",".").toDouble
                            if (d%1 == 0.0)
                              IntType(d.toInt)
                            else
                              NumType(d)
                          } else
                            StrType(s)
                      case _: str.type => StrType(s)
                    }
                  }
                }
              if (rs.wasNull()) None else Some(cellValue)
            }
          )
      }
    }.toList.map(sc => seqCellToMap(sc))
  }

  /**
   * Optimization scrollable r.s.
   * https://www.informit.com/articles/article.aspx?p=26251&seqNum=7
  */
  private def execFunctionCursor(conn: Connection, reqHeader: RequestHeader, query: Query): rows = {
    conn.setClientInfo("OCSID.MODULE", "WS_CONN")
    conn.setClientInfo("OCSID.ACTION", "FUNC_CURSOR")
    execGlobalCursor(conn, reqHeader)
    val call :CallableStatement = conn.prepareCall (s"{ ? = call ${query.query}}")
    call.registerOutParameter (1, OracleTypes.CURSOR);
    call.execute()
    val rs :ResultSet = call.getObject(1).asInstanceOf[ResultSet]
    val rows = getRowsFromResultSet(rs,query.convtype)
    rows
  }

  private def execFunctionSimple(conn: Connection, reqHeader: RequestHeader, query: Query): rows = {
    conn.setClientInfo("OCSID.MODULE", "WS_CONN")
    conn.setClientInfo("OCSID.ACTION", "FUNC_SIMPLE")
    execGlobalCursor(conn, reqHeader)
    val rs :ResultSet = conn.createStatement.executeQuery(s"select ${query.query} as result from dual")
    val rows = getRowsFromResultSet(rs,query.convtype)
    rows
  }

  private def execSimpleQuery(conn: Connection, reqHeader: RequestHeader, query: Query): rows = {
    conn.setClientInfo("OCSID.MODULE", "WS_CONN")
    conn.setClientInfo("OCSID.ACTION", "SELECT")
    execGlobalCursor(conn, reqHeader)
    val rs :ResultSet = conn.createStatement.executeQuery(s"${query.query}")
    val rows = getRowsFromResultSet(rs,query.convtype)
    rows
  }

  private def execProcCursor(conn: Connection, reqHeader: RequestHeader, query: Query): rows = {
    conn.setClientInfo("OCSID.MODULE", "WS_CONN")
    conn.setClientInfo("OCSID.ACTION", "PROC_CURSOR")
    execGlobalCursor(conn, reqHeader)
    val call :CallableStatement = conn.prepareCall(s"begin ${query.query}; end;")
    call.registerOutParameter (1, OracleTypes.CURSOR);
    call.execute()
    val rs :ResultSet = call.getObject(1).asInstanceOf[ResultSet]
    val rows = getRowsFromResultSet(rs,query.convtype)
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
      case _ => Task(List[ListMap[String,Option[CellType]]]())
    }).catchSome{
      case se: java.sql.SQLException =>
        Task {
          println("~~~~~~~~~~~~~~~~~~~~~ CLOSING CONN WITH SQL Exception ~~~~~~~~~~~~~~~~~~~")
          conn.setClientInfo("OCSID.ACTION", "SQL_EXCP")
          conn.close()
        } *>
        log.error(s"${query.qt} SQLException Code: ${se.getErrorCode} ${se.getLocalizedMessage} ") *>
          Task.fail(new java.sql.SQLException(se))
    }
    /**
     * The close method closes connections and automatically returns them to the pool.
     * The close method does not physically remove the connection from the pool.
    */
    currTs <- ZIO.accessM[Clock](_.get.currentTime(TimeUnit.MILLISECONDS))
    ddr <- Task(DictDataRows(query.name, (currTs-beginTs).toFloat/1000, "db", rows))
    _ <- Task{if (!conn.isClosed)
        conn.close()
    }
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
            dictRows.copy(src = "cache",time = 0L), reqQuery.reftables.getOrElse(Seq())))
          .when(reqHeader.nocache.forall(_ == 0) &&
            reqQuery.nocache.forall(_ == 0))
      } yield dictRows


  val getDbResultSet: ( Query, RequestHeader) => ZIO[ZEnvConfLogCache, Throwable, DictDataRows] =
    ( trqDict, reqHeader) =>
      for {
        beginTs <- clock.currentTime(TimeUnit.MILLISECONDS)
        cache <- ZIO.access[CacheManager](_.get)
        valFromCache: Option[CacheEntity] <- cache.get(reqHeader.hashCode() + trqDict.hashCode()) //todo: ??!!
        currTs <- clock.currentTime(TimeUnit.MILLISECONDS)
        dictRows <- valFromCache match {
          case Some(s: CacheEntity) =>
            log.trace(s"--- [VALUE GOT FROM CACHE] [${s.dictDataRows.name}] ---") *>
              ZIO.succeed(s.dictDataRows.copy(time = (currTs - beginTs).toFloat/1000))
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
    //TODO: orElse get from common pool if dedicated connection is crashed.
    conn <- ucp.getConnection
    nts <- Task(getChangedTables(conn)).catchSome {
      case se: java.sql.SQLException =>
        Task {
              conn.setClientInfo("OCSID.ACTION", "FAIL")
              conn.close()
            } *>
          log.error(s"getChangedTables FAIL, SQLException Code: ${se.getErrorCode} ${se.getLocalizedMessage} ") *>
          Task(List[String]())
    }
    _ <- Task{if (!conn.isClosed)
      conn.close()
    }
  } yield nts

}
