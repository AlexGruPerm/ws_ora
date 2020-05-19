package reqdata

import io.circe.Decoder
import io.circe.generic.JsonCodec


sealed trait queryType
case object func_simple extends queryType
case object func_cursor extends queryType
case object proc_cursor extends queryType
case object select extends queryType
case object unknown extends queryType

import io.circe._
import io.circe.parser._
object CustDecoders {

  val r = scala.util.Random

  //todo: refactor c.c. name Dict into Query
  implicit val decoderDict: Decoder[Query] = Decoder.instance { h =>
    for {
      name <- h.get[String]("name")
      qt <- h.get[Option[String]]("qt")
      query <- h.get[String]("query")
      rt <- h.get[Option[Seq[String]]]("reftables")
      qtType = qt.getOrElse("unsettled").toLowerCase match {
        case "func_simple" => func_simple
        case "func_cursor" => func_cursor
        case "proc_cursor" => proc_cursor
        case "select" => select
        case _ => unknown
      }
    } yield Query(name,qtType,query,rt)
  }

  implicit val decoderRequestData: Decoder[RequestData] = Decoder.instance { h =>
    for {
      user_session <- h.get[String]("user_session")
      cont_encoding_gzip_enabled <- h.get[Int]("cont_encoding_gzip_enabled")
      thread_pool<- h.get[String]("thread_pool")
      request_timeout_ms <- h.get[Double]("request_timeout_ms")
      nocache <- h.get[Option[Int]]("nocache")
      context <- h.get[Option[String]]("context")
      queries <- h.get[Seq[Query]]("queries")
    } yield RequestData(RequestHeader(user_session,cont_encoding_gzip_enabled,thread_pool,request_timeout_ms,nocache,context),queries)
  }

}

/**
*/
case class Query(
                 name: String,
                 qt : queryType,
                 query :String,
                 reftables: Option[Seq[String]]
               )

case class RequestHeader(
                          user_session: String,
                          cont_encoding_gzip_enabled: Int, //use gzip or not for response json (Content-Encoding)
                          thread_pool: String, //block or sync
                          request_timeout_ms: Double, //client can set request timeout, after t.o. return json response with error
                          nocache : Option[Int],
                          context: Option[String]
                        )

/**
*/
case class RequestData(
                        header: RequestHeader,
                        queries: Seq[Query]
                      )




