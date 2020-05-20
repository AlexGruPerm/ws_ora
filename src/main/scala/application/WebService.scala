package application

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl._
import akka.http.scaladsl.model.{HttpRequest, _}
import akka.util.Timeout
import env.EnvContainer.{IncConnSrvBind, ZEnvConfLogCache, ZEnvLogCache}
import zio.logging.log
import zio.{Runtime, _}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.language.postfixOps
import CacheHelper._
import UcpHelper._
import StatHelper._
import wsconfiguration.ConfClasses.{DbConfig, WsConfig}
import zio.config.Config

object WebService {

  private val notifTimeout: Int = 3000

  /**
   * Read config file and open Http server.
   * Example :
   * https://medium.com/@ghostdogpr/combining-zio-and-akka-to-enable-distributed-fp-in-scala-61ffb81e3283
   *
   */
  val startService : ZIO[ZEnvConfLogCache, Throwable, Unit] = {
    import zio.duration._
    Managed.make(Task(ActorSystem("WsDb")))(sys => Task.fromFuture(_ => sys.terminate()).ignore).use(
      actorSystem =>
        for {
          _ <- CacheLog.out("WsServer",true)
          //cfg <- ZIO.access[Config[WsConfig]](_.get)

          fiber <- startRequestHandler(actorSystem).forkDaemon
          _ <- fiber.join

          cacheCheckerValidator <-
              //todo: temporary commented.
              //validate cache by ref-tables.
              //cacheValidator.repeat(Schedule.spaced(3.second)).forkDaemon *>
              //clean cache
              cacheChecker.repeat(Schedule.spaced(5.second)).forkDaemon *>
                ucpMonitor.repeat(Schedule.spaced(10.second)).forkDaemon *>
                statMonitor.repeat(Schedule.spaced(5.second)).forkDaemon
              //readUserInterrupt(fiber, actorSystem).repeat(Schedule.spaced(1.second)).forkDaemon

          _ <- cacheCheckerValidator.join //todo: may be divide on 2 separate forkDeamon
        } yield ()
    )
  }


  val serverSource: ActorSystem => ZIO[ZEnvConfLogCache, Throwable, IncConnSrvBind] = actorSystem =>
    for {
      _ <- CacheLog.out("serverSource",true)
      conf <- ZIO.access[Config[WsConfig]](_.get)
      _ <- log.info(s"Create Source[IncConnSrvBind] with ${conf.api.endpoint}:${conf.api.port}") &&&
           log.info(s" In input config are configured DB SID = ${conf.dbconf.sid} ")
      ss <- Task(Http(actorSystem).bind(interface = conf.api.endpoint, port = conf.api.port))
    } yield ss


  /**
   * dbConfigList are registered list of databases from config file - application.conf
  */
  def reqHandlerM(dbConfigList: DbConfig, actorSystem: ActorSystem, rt: Runtime[ZEnvConfLogCache])(request: HttpRequest):
  Future[HttpResponse] = {
    implicit val system: ActorSystem = actorSystem

    import scala.concurrent.duration._
    implicit val timeout: Timeout = Timeout(10 seconds)
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    import akka.http.scaladsl.unmarshalling.Unmarshal
    import ReqResp._

    lazy val responseFuture: ZIO[ZEnvConfLogCache, Throwable, HttpResponse] = request
    match {
      case request@HttpRequest(HttpMethods.POST, Uri.Path("/dicts"), _, _, _) =>
        val reqEntityString: Future[String] = Unmarshal(request.entity).to[String]
        routeQueries(request, dbConfigList, reqEntityString)
      case request@HttpRequest(HttpMethods.GET, _, _, _, _) =>
        request match {
          case request@HttpRequest(_, Uri.Path("/debug"), _, _, _) => routeGetDebug(request)
          case request@HttpRequest(_, Uri.Path("/favicon.ico"), _, _, _) => routeGetFavicon(request)
        }
      case request: HttpRequest =>
        request.discardEntityBytes()
        route404(request)
    }
    rt.unsafeRunToFuture(
      responseFuture
    )
  }



  /**
   * 1) runForeach(f: Out => Unit): Future[Done] - is a method of class "Source"
   *
   * 2)
   * handleWithAsyncHandler - is a method of class "IncomingConnection"
   * and it wait input parameter:
   * (handler: HttpRequest => Future[HttpResponse])
   *
   */
  val startRequestHandler: ActorSystem => ZIO[ZEnvConfLogCache, Throwable, Future[Done]] = actorSystem => {
      implicit val system: ActorSystem = actorSystem
      import scala.concurrent.duration._
      implicit val timeout: Timeout = Timeout(120 seconds)
      implicit val executionContext: ExecutionContextExecutor = system.dispatcher
      import akka.stream.scaladsl.Source
      for {
        _ <- CacheLog.out("startRequestHandler", true)
        conf <- ZIO.access[Config[WsConfig]](_.get)
        rti: Runtime[ZEnvConfLogCache] <- ZIO.runtime[ZEnvConfLogCache]
        ss: Source[Http.IncomingConnection, Future[ServerBinding]] <- serverSource(actorSystem)
        reqHandlerFinal <- RIO(reqHandlerM(conf.dbconf, actorSystem, rti) _)
        serverWithReqHandler =
        ss.runForeach {
          conn =>
            conn.handleWithAsyncHandler(
              r => rti.unsafeRun(ZIO(reqHandlerFinal(r)))
            )
        }
        sourceWithServer <- ZIO.succeed(serverWithReqHandler)
      } yield sourceWithServer
    }




}

