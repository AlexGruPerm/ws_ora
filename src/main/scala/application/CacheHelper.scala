package application

import java.io.IOException
import java.sql.{CallableStatement, ResultSet}

import akka.Done
import akka.actor.ActorSystem
import data.{CacheEntity, Notification, reftable}
import db.DbExecutor
import db.DbExecutor.Notifications
import db.Ucp.UcpZLayer
import env.CacheObject.CacheManager
import env.EnvContainer.{ZEnvConfLogCache, ZEnvLogCache}
import oracle.jdbc.OracleTypes
import reqdata.CustDecoders.strToReftable
import wsconfiguration.ConfClasses.WsConfig
import zio.{Schedule, UIO, ZIO}
import zio.logging.log
import zio._

import scala.concurrent.{ExecutionContextExecutor, Future}

object CacheHelper {

  /**
   * Using for cleaning cache by removing entities from.
   */
   val cacheChecker: ZIO[ZEnvLogCache, Nothing, Unit] =
    for {
      _ <- CacheLog.out("cacheChecker",true)
      _ <- CacheClean.checkAndClean
    } yield ()

  /**
   * Search Entity in Cache by tablename in  and remove it
   */
  private val removeFromCacheByRefTable: reftable => ZIO[ZEnvLogCache, Throwable, Unit] = tableName =>
    for {
      cache <- ZIO.access[CacheManager](_.get)
      cv <- cache.getCacheValue
      //search keys for removing by tableName
      //foundKeys: Seq[Int] = hashKeysForRemove(cv.dictsMap, tableName)
      foundKeys <- hashKeysForRemove(cv.dictsMap, tableName)
      _ <- if (foundKeys.nonEmpty)
        log.info(s"keys for removing from cache $foundKeys")
      else
        UIO.succeed(())
      _ <- cache.remove(foundKeys)
    } yield ()


  val cacheCleaner: Notifications => ZIO[ZEnvLogCache, Nothing, Unit] = notifications =>
    for {//todo: maybe foreachPar for performance!?
      _ <- ZIO.foreach(notifications) { nt =>
          for {
            _ <- log.trace(s"Notif: $nt")
            _ <- removeFromCacheByRefTable(strToReftable(nt))
          } yield ()
      }.catchAllCause {
        e => log.error(s" cacheValidator Exception $e")
      }
    } yield ()


  /**
   **
   *CREATE OR REPLACE FUNCTION notify_change() RETURNS TRIGGER AS $$
   * BEGIN
   * perform pg_notify('change', TG_TABLE_NAME);
   * RETURN NULL;
   * END;
   * $$ LANGUAGE plpgsql;
   **
   *drop TRIGGER trg_ch_listener_notify on listener_notify;
   **
   *create TRIGGER trg_ch_listener_notify
   * AFTER INSERT OR UPDATE OR DELETE ON listener_notify
   * FOR EACH statement EXECUTE PROCEDURE notify_change();
   *
   */
  val cacheValidator:  ZIO[ZEnvConfLogCache, Throwable, Unit] = {
    import zio.duration._
    for {
      notifs <- DbExecutor.getNotifications
      //_ <- log.trace("cacheValidator")
      _ <- cacheCleaner(notifs)
    } yield ()
  }

  /**
   * used
   * Check that CacheEntity.reftables depends on tableName.
   * tableName is a pair "schema_name.table_name"
   * reftables elements can contains (3 ytpes):
   * "schema_name.table_name" - exact value
   * "schema_name.*" - any table in schema
   * "*.*" - any table in any schema.
  */
  private def isCecheEntDependsOnTable(reftables: Seq[reftable], rt: reftable): Boolean =
    reftables.contains(rt) ||
      reftables.contains(reftable("*", "*")) ||
      reftables.contains(reftable(rt.schema,"*"))

  /**
   * Is field reftables from Class CacheEntity contain given tableName
   */
  private def hashKeysForRemove(dictsMaps: Map[Int, CacheEntity], rt: reftable) :ZIO[ZEnvLogCache, Nothing, Seq[Int]] =
    UIO(dictsMaps.mapValues(rts => isCecheEntDependsOnTable(rts.reftables,rt)).withFilter(_._2).map(_._1).toSeq)

  /**
   * Read user input and raise Exception if not empty.
   * https://github.com/zio/zio/pull/113
   * https://github.com/zio/zio/issues/74
   */
    import zio.duration._
  val readUserInterrupt: (Fiber.Runtime[Throwable, Future[Done]], ActorSystem) =>
    ZIO[ZEnvConfLogCache, Throwable, Unit] = (fiber, actorSystem) => {
    implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
    for {
      userInput <- zio.console.getStrLn
      exit <- if (userInput.nonEmpty) {
        log.error(s"-----------------------------------------------------------") *>
        log.error(s"User interrupt web service by console input = $userInput") *>
        log.error(s"-----------------------------------------------------------") *>
        ZIO.sleep(3.seconds) *>
        Task.fail(new Exception("Web service interrupted by user."))
      }
      else {
        UIO.succeed(())
      }
    } yield exit
  }

}
