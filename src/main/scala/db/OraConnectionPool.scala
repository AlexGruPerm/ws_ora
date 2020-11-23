package db

import oracle.ucp.jdbc.{PoolDataSourceFactory, ValidConnection}
import wsconfiguration.ConfClasses.{DbConfig, UcpConfig}


/**
 * Universal Connection Pool Developer's Guide
 * https://docs.oracle.com/en/database/oracle/oracle-database/12.2/jjucp/toc.htm
 * Also read 4.3 About Optimizing Real-World Performance with Static Connection Pools
 */
class OraConnectionPool(conf: DbConfig, props: UcpConfig){
  val pds = PoolDataSourceFactory.getPoolDataSource
  pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource")
  pds.setURL(s"jdbc:oracle:thin:@//${conf.ip}:${conf.port}/${conf.sid}")
  pds.setUser(conf.username)
  pds.setPassword(conf.password)
  pds.setConnectionPoolName(props.ConnectionPoolName)
  pds.setConnectionWaitTimeout(props.ConnectionWaitTimeout)
  pds.setInitialPoolSize(props.InitialPoolSize)
  // A connection pool always tries to return to the minimum pool size
  pds.setMinPoolSize(props.MinPoolSize)
  //The maximum pool size property specifies the maximum number of available
  // and borrowed (in use) connections that a pool maintains.
  pds.setMaxPoolSize(props.MaxPoolSize)
  pds.setAbandonedConnectionTimeout(props.AbandonConnectionTimeout)
  pds.setInactiveConnectionTimeout(props.InactiveConnectionTimeout)
  //todo: check it.
  //https://docs.oracle.com/en/database/oracle/oracle-database/12.2/jjucp/validating-ucp-connections.html#GUID-A7C850D6-4026-4629-BCFA-9181C29EFBF9
  pds.setValidateConnectionOnBorrow(true)

  val jdbcVersion: String = {
    val conn: java.sql.Connection = pds.getConnection
    conn.setClientInfo("OCSID.MODULE", "WS")
    conn.setClientInfo("OCSID.ACTION", "META")
    val md: java.sql.DatabaseMetaData = conn.getMetaData
    val jdbcVers: String = md.getJDBCMajorVersion+"."+ md.getJDBCMinorVersion
    conn.close()
    jdbcVers
  }

  def closePoolConnections: Unit = (1 to pds.getAvailableConnectionsCount + pds.getBorrowedConnectionsCount)
    .foreach(_ => {
      val c = pds.getConnection()
      println("closing this connection")
      c.asInstanceOf[ValidConnection].setInvalid()
      c.close()
    }
    )

}