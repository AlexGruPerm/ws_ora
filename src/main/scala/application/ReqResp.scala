package application

import java.io.{File, IOException}

import akka.http.scaladsl.model.HttpCharsets._
import akka.http.scaladsl.model.MediaTypes.{`application/json`, `text/html`}
import akka.http.scaladsl.model.headers.`Content-Encoding`
import akka.http.scaladsl.model.HttpRequest
import akka.stream.scaladsl.FileIO
import akka.util.ByteString
import data.{DbErrorDesc, DictsDataAccum, RequestResult}
import env.CacheObject.CacheManager
import env.EnvContainer.{ZEnvLog, ZEnvLogCache}
import io.circe.parser.parse
import io.circe.Printer
import zio.{IO, Task, UIO, URIO, ZIO}
import zio.console.putStrLn

import scala.concurrent.{Await, Future}
import scala.io.{BufferedSource, Source}
import scala.language.postfixOps
import reqdata.{NoConfigureDbInRequest, ReqParseException, RequestData}
import testsjsons.CollectJsons
import wsconfiguration.ConfClasses.DbConfig
import zio.logging.log


/**
 * monitor sessions from wsfphp:
 * select *
 * from   pg_stat_activity p
 * where  coalesce(p.usename,'-')='prm_salary'
 *   and application_name='wsfphp'
 *
*/
object ReqResp {

  val pnf:Int = 404

  val logRequest: HttpRequest => ZIO[ZEnvLog, Throwable, Unit] = request => for {
    _ <- log.trace(s"================= ${request.method} REQUEST ${request.protocol.value} =====")
    _ <- log.trace(s"uri : ${request.uri} ")
    _ <- log.trace("  ---------- HEADER ---------")
    _ <- URIO.foreach(request.headers.zipWithIndex)(hdr => log.trace(s"   #${hdr._2} : ${hdr._1.toString}"))
    _ <- log.trace(s"  ---------------------------")
    //_ <- log.trace(s"entity ${request.entity.toString} ")
    _ <- log.trace("========================================================")
  } yield ()


  val logReqData: Task[RequestData] => ZIO[ZEnvLog, Throwable, Unit] = reqData => for {
    rd <- reqData
    _ <- log.trace("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    _ <- log.trace(s"session_id = ${rd.user_session}")
    _ <- log.trace(s"encoding_gzip = ${rd.cont_encoding_gzip_enabled}")
    _ <- log.trace(s"dicts size = ${rd.dicts.size}")
    _ <- URIO.foreach(rd.dicts) { d =>
      log.trace(s"dict = ${d.db} - ${d.proc} ")
      /*
      for {
        _ <- log.trace(s" ref tables in dict : ${d.reftables.getOrElse(Seq()).size} ")
        _ <- URIO.foreach(d.reftables.getOrElse(Seq())) { tableName =>
          log.trace(s"  reftable = $tableName ")
        }
      } yield URIO.unit
      */
    }
    _ <- log.trace("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
  } yield ()

  /**
   * This is one of client handler.
   * Inside it opens 1-* database connections for parallel executing queries.
   * Sometimes it can be Exception:
   * org.postgresql.util.PSQLException: FATAL: remaining connection slots are reserved for
   * non-replication superuser connection
   * And we need catch it and return response json to client like this :
   * {
   *  "status" : "error",
   *  "message" : "remaining connection slots are reserved for non-replication superuser connections",
   *  "exception class" : "PSQLException"
   * }
  */
  import akka.http.scaladsl.coding.Gzip
  import akka.http.scaladsl.model._, headers.HttpEncodings

  private def compress(usingGzip: Int, input: String) :ByteString =
    if (usingGzip==1) Gzip.encode(ByteString(input)) else ByteString(input)

  import io.circe.syntax._
  import scala.concurrent.duration._
  private val parseRequestData: Future[String] => Task[RequestData] = futString => {
    val strRequest: String  = Await.result(futString, 3 second)
    parse(strRequest) match {
      case Left (failure) => Task.fail (
        ReqParseException("Error code[001] Invalid json in request", failure.getCause)
      )
      case Right (json) => json.as[RequestData].swap
      match {
        case   Left(sq) => Task.succeed(sq)
        case   Right(failure) =>  Task.fail (
          ReqParseException("Error code[002] Invalid json in request", failure.getCause)
        )
      }
      }
    }


  /**
   * Function to check in one place that all dicts.db exist among configured db list (application.conf)
  */
  val dictDbsCheckInConfig: (Task[RequestData], DbConfig) => ZIO[ZEnvLog, Throwable, Unit] =
    (requestData, configuredDB) =>
      for {
        //logChecker <- ZIO.access[Logging](_.logger)xxxx
        reqData <- requestData
        reqListDb: Seq[String] = reqData.dicts.map(_.db).distinct
        accumRes <- ZIO.foreachPar(reqListDb){thisDb =>
          if (1==1)/*(configuredDB.name == thisDb) */{
            ZIO.none
        } else {
            ZIO.some(s"DB [$thisDb] from request not found in config file application.conf")
          }}

        checkResult <- if (accumRes.flatten.nonEmpty) {
          Task.fail(
            NoConfigureDbInRequest(accumRes.flatten.head)
          )
        } else {Task.succeed(())
        }

      } yield checkResult




/*    import zio.blocking._
    val routeDicts: (HttpRequest, DbConfig, Future[String]) => ZIO[ZEnvLogCache, Throwable, HttpResponse] =
      (request, configuredDbList, reqEntity) =>
      for {
        cache <- ZIO.access[CacheManager](_.get)
        cv <- cache.getCacheValue
        _ <- log.trace("==========================================")
        _ <- log.trace(s"START - routeDicts HeartbeatCounter = ${cv.HeartbeatCounter} " +
          s"bornTs = ${cv.cacheCreatedTs}")
        _ <- cache.addHeartbeat

        _ <- logRequest(request)
        reqRequestData = parseRequestData(reqEntity)
        _ <- logReqData(reqRequestData)
        seqResDicts <- reqRequestData
        //check that all requested db are configures.
        resString :ByteString <- dictDbsCheckInConfig(reqRequestData, configuredDbList)
          .foldM(
            checkErr => {
              val failJson =
                DbErrorDesc("error", checkErr.getMessage, "Cause of exception", checkErr.getClass.getName).asJson
              Task(compress(seqResDicts.cont_encoding_gzip_enabled, Printer.spaces2.print(failJson)))
            },
            _ =>
              for {
                str <- ZIO.foreachPar(seqResDicts.dicts) {
                  thisDict =>
                    if (seqResDicts.thread_pool == "block") {
                      //run in separate blocking pool, "unlimited" thread count
                      blocking(DbExecutor.getDict(configuredDbList, thisDict))
                    } else {
                      //run on sync pool, count of threads equal CPU.cores*2 (cycle)
                      DbExecutor.getDict(configuredDbList, thisDict)
                    }
                }.fold(
                  err => compress(seqResDicts.cont_encoding_gzip_enabled,
                    Printer.spaces2.print(DbErrorDesc("error", err.getMessage, "method[routeDicts]",
                      err.getClass.getName).asJson)
                  ),
                  succ => compress(seqResDicts.cont_encoding_gzip_enabled,
                    Printer.spaces2.print(RequestResult("ok", DictsDataAccum(succ)).asJson)
                  )
                )
              } yield str
          )

        // Common logic
        resEntity <- Task(HttpEntity(`application/json`.withParams(Map("charset" -> "UTF-8")), resString))
        httpResp <- Task(HttpResponse(StatusCodes.OK, entity = resEntity))

        httpRespWithHeaders =
        if (seqResDicts.cont_encoding_gzip_enabled == 1) {
          httpResp.addHeader(`Content-Encoding`(HttpEncodings.gzip))
        } else {
          httpResp
        }

        resFromFuture <- ZIO.fromFuture { implicit ec =>
          Future.successful(httpRespWithHeaders).flatMap {
            result: HttpResponse => Future(result).map(_ => result)
          }
        }

    } yield resFromFuture*/

  private def openFile(s: String): Task[BufferedSource]= // IO[IOException, BufferedSource] =
    IO.effect(Source.fromFile(s)).refineToOrDie[IOException]

  private def closeFile(f: BufferedSource): UIO[Unit] =
    UIO.unit

  val routeGetDebug: HttpRequest => ZIO[ZEnvLog, Throwable, HttpResponse] = request => for {
    strDebugForm <- openFile("debug_post.html").bracket(closeFile) {
      file =>Task(file.getLines.mkString.replace("req_json_text", CollectJsons.reqJsonText_))
    } orElse
      openFile("C:\\ws_fphp\\src\\main\\resources\\debug_post.html").bracket(closeFile) {
        file =>Task(file.getLines.mkString.replace("req_json_text", CollectJsons.reqJsonText_))
      }


    _ <- logRequest(request)

    f <- ZIO.fromFuture { implicit ec =>
      Future.successful(
        HttpResponse(StatusCodes.OK, entity = HttpEntity(`text/html` withCharset `UTF-8`, strDebugForm)))
        .flatMap {
          result: HttpResponse => Future(result).map(_ => result)
        }
    }
  } yield f


  val routeGetFavicon: HttpRequest => ZIO[ZEnvLog, Throwable, HttpResponse] = request => for {
    _ <- putStrLn(s"================= ${request.method} REQUEST ${request.protocol.value} =============")
    icoFile <- Task{new File("C:\\ws_fphp\\src\\main\\resources\\favicon.png")}
    f <- ZIO.fromFuture { implicit ec =>
      Future.successful(
        HttpResponse(StatusCodes.OK, entity =
          HttpEntity(MediaTypes.`application/octet-stream`, icoFile.length, FileIO.fromPath(icoFile.toPath))
        )
      ).flatMap{
          result :HttpResponse => Future(result).map(_ => result)
        }
    }
  } yield f


  val route404: HttpRequest => ZIO[ZEnvLog, Throwable, HttpResponse] = request => for {
    _ <- logRequest(request)
    f <- ZIO.fromFuture { implicit ec =>
      Future.successful(HttpResponse(pnf, entity = "Unknown resource!"))
        .flatMap{
          result :HttpResponse => Future(result).map(_ => result)
        }
    }
  } yield f


}