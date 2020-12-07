package data

import io.circe.generic.JsonCodec
//import io.circe.generic.JsonCodec, io.circe.syntax._

final case class DbErrorException(private val message: String = "",
                                  private val cause: Throwable = None.orNull,
                                  query: String = "no info")
  extends Exception(message, cause)

/**
 * describe db error for responding to client as json.
*/
@JsonCodec case class DbErrorDesc(
                                   status: String,
                                   message: String,
                                   statement: String,
                                   cause: String,
                                   exception: String,
                                   query: String = "")

