package env

import scala.language.higherKinds
import wsconfiguration.ConfClasses.WsConfig
import zio._
import zio.config.magnolia.DeriveConfigDescriptor._
import zio.config.typesafe.TypesafeConfig

object ConfigLayerObject {

  val wsConfigAutomatic = descriptor[WsConfig]

  def configLayer(confFileName: String): Layer[Throwable, config.Config[WsConfig]] =
    TypesafeConfig.fromHoconFile(new java.io.File(confFileName), wsConfigAutomatic)

}



