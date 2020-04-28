package reqdata

import io.circe.Decoder
import io.circe.generic.JsonCodec


sealed trait queryType
case object func extends queryType
case object proc extends queryType
case object select extends queryType
case object unknown extends queryType

import io.circe._
import io.circe.parser._
object CustDecoders {

  //todo: refactor c.c. name Dict into Query
  implicit val decoderDict: Decoder[Query] = Decoder.instance { h =>
    for {
      name <- h.get[String]("name")
      qt <- h.get[Option[String]]("qt")
      rt <- h.get[Option[Seq[String]]]("reftables")
      qtRes = qt.getOrElse("unsettled").toLowerCase match {
        case "func" => func
        case "proc" => proc
        case "select" => select
        case _ => unknown
      }
    } yield Query(name,qtRes,rt)
  }

  implicit val decoderRequestData: Decoder[RequestData] = Decoder.instance { h =>
    for {
      user_session <- h.get[String]("user_session")
      cont_encoding_gzip_enabled <- h.get[Int]("cont_encoding_gzip_enabled")
      thread_pool<- h.get[String]("thread_pool")
      request_timeout_ms <- h.get[Double]("request_timeout_ms")
      cache_live_time <- h.get[Option[Long]]("cache_live_time")
      context <- h.get[Option[String]]("context")
      queries <- h.get[Seq[Query]]("queries")
    } yield RequestData(user_session,cont_encoding_gzip_enabled,thread_pool,request_timeout_ms,cache_live_time,context,queries)
  }

}


/**
*/
//@JsonCodec
case class Query(
                 name: String,
                 qt : queryType,
                 reftables: Option[Seq[String]]
               )

/**
*/
//@JsonCodec
case class RequestData(
                        user_session: String,
                        cont_encoding_gzip_enabled: Int, //use gzip or not for response json (Content-Encoding)
                        thread_pool: String, //block or sync
                        request_timeout_ms: Double, //client can set request timeout, after t.o. return json response with error
                        cache_live_time: Option[Long],//0 - no cache, otherwise set live time for each dict in cache. todo: Future set individual.
                        context: Option[String],
                        queries: Seq[Query]
                      )




