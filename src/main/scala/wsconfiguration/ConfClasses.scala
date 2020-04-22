package wsconfiguration

/**
 * Object only for store Config case classes.
*/
object ConfClasses {

  final case class ApiConfig(
                              endpoint: String,
                              port: Int
                            )

  final case class DbConfig(
                             ip: String,
                             port: Int = 1521,
                             sid: String,
                             username: String,
                             password: String
                           )

  final case class UcpConfig(
                              ConnectionPoolName: String,
                              InitialPoolSize: Int ,
                              MinPoolSize: Int ,
                              MaxPoolSize: Int,
                              ConnectionWaitTimeout: Int
                            )

  final case class WsConfig(
                             api: ApiConfig,
                             dbconf: DbConfig,
                             ucpconf: UcpConfig
                           )

}