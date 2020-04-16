import java.sql.ResultSet
import oracle.ucp.jdbc.PoolDataSourceFactory

final case class DbConfig(
                           ip: String,
                           port: Int = 1521,
                           sid: String,
                           username: String,
                           password: String
                         )

final case class ConnPoolProperties(
                                     ConnectionPoolName: String,
                                     InitialPoolSize: Int,
                                     MinPoolSize: Int,
                                     MaxPoolSize: Int
                                   )

/**
 * Universal Connection Pool Developer's Guide
 * https://docs.oracle.com/en/database/oracle/oracle-database/12.2/jjucp/toc.htm
 * Also read 4.3 About Optimizing Real-World Performance with Static Connection Pools
*/
class OraConnectionPool(conf: DbConfig, props: ConnPoolProperties){
  val pds = PoolDataSourceFactory.getPoolDataSource
  pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource")
  pds.setURL(s"jdbc:oracle:thin:@//${conf.ip}:${conf.port}/${conf.sid}")
  pds.setUser(conf.username)
  pds.setPassword(conf.password)
  pds.setConnectionPoolName(props.ConnectionPoolName)
  pds.setInitialPoolSize(props.InitialPoolSize)
  pds.setMinPoolSize(props.MinPoolSize)
  pds.setMaxPoolSize(props.MaxPoolSize)

  def closeall :Unit =
    (1 to pds.getAvailableConnectionsCount).toList.foreach(_ => pds.getConnection.close())
}

    val dbconf = DbConfig("10.127.24.11", 1521, "test", "MSK_ARM_LEAD", "MSK_ARM_LEAD")
    val cpp = ConnPoolProperties("ChangeListener",1,1,1)
    val oracp = new OraConnectionPool(dbconf,cpp)
    val con = oracp.pds.getConnection()

    val stmt = con.createStatement()
    val rs = stmt.executeQuery("select id,s_name,view_src_data_name from d_data_source where id<=10")

    val columns: List[(String, String)] = (1 to rs.getMetaData.getColumnCount)
      .map(cnum => (rs.getMetaData.getColumnName(cnum), rs.getMetaData.getColumnTypeName(cnum))).toList

    columns.foreach(c => println(s"${c._1} - ${c._2}" ))

    val rows = Iterator.continually(rs).takeWhile(_.next()).map { rs =>
      columns.map(
        cname => /*DictRow*/(cname._1, rs.getString(cname._1))
      )
    }.toList

    rows.foreach(r => println(r))

