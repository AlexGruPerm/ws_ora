package logs

import env.EnvContainer.ZEnvConfLogCache
import wsconfiguration.ConfClasses.WsConfig
import zio.ZIO
import zio.config.Config
import zio.logging.log

object LogHelpers {

   def outputInitalConfig: ZIO[ZEnvConfLogCache, Throwable, Unit] =
    for {
      cfg <- ZIO.access[Config[WsConfig]](_.get)
      _ <- log.info(" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ")
      _ <- log.info(" WebService ")
      _ <- log.info(s"    ${cfg.api.endpoint}:${cfg.api.port} ")
      _ <- log.info(" Database ")
      _ <- log.info(s"    ip:port/sid = ${cfg.dbconf.ip}:${cfg.dbconf.port}/${cfg.dbconf.sid}")
      _ <- log.info(s"    username/password = ${cfg.dbconf.username}/******* ")
      _ <- log.info(" Universal Connection Pool (UCP ) ")
      _ <- log.info(s"    https://docs.oracle.com/en/database/oracle/oracle-database/12.2/jjucp/toc.htm ")
      _ <- log.info(s"    ConnectionPoolName : ${cfg.ucpconf.ConnectionPoolName}")
      _ <- log.info(s"    InitialPoolSize : ${cfg.ucpconf.InitialPoolSize}")
      _ <- log.info(s"    (Min/Max)PoolSize : ${cfg.ucpconf.MinPoolSize}/${cfg.ucpconf.MaxPoolSize}")
      _ <- log.info(" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ")
    } yield ()

}
