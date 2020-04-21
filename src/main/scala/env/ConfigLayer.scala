package env

import scala.language.higherKinds
import wsconfiguration.ConfClasses.WsConfig
import java.io.File
import zio._
import zio.console._
import zio.config.magnolia.DeriveConfigDescriptor._
import zio.ZLayer._
import zio.config.Config
import zio.config.typesafe.TypesafeConfig
import zio.console.Console
import zio.{App, ZEnv, ZIO}

object ConfigLayerObject {

  val wsConfigAutomatic = descriptor[WsConfig]

  def configLayer(confFileName: String): Layer[Throwable, config.Config[WsConfig]] =
    TypesafeConfig.fromHoconFile(new java.io.File(confFileName), wsConfigAutomatic)

}



