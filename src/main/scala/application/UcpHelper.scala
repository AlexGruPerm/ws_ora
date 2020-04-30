package application

import db.Ucp.UcpZLayer
import env.EnvContainer.{ZEnvConfLogCache, ZEnvLogCache}
import zio.ZIO
import zio.logging.log

object UcpHelper {

  /**
   *
   */
  val ucpMonitor: ZIO[ZEnvConfLogCache, Nothing, Unit] =
    for {
      ucp <- ZIO.access[UcpZLayer](_.get)
      ac <- ucp.getAvailableConnectionsCount
      bc <- ucp.getBorrowedConnectionsCount
      _ <- log.info(s"~~~~~~~~~~ UCP CURRENT STATISTIC ~~~~~~~~~~~~~~~~~")
      _ <- log.info(s"TOTAL : ${ac+bc} AVAILABLE : $ac BORROWED : $bc")
      _ <- log.info(s"~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    } yield ()

}
