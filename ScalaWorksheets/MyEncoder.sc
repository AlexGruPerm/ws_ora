import io.circe.{Encoder, Json, Printer}
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe._

sealed trait CellType
final case class StrType(s: String) extends CellType
final case class IntType(i: Int) extends CellType

object CellType {
  implicit val encoder: Encoder[CellType] = Encoder.instance {
    case StrType(s) => Json.fromString(s)
    case IntType(i) => Json.fromInt(i)
  }
}

case class DictDataRows(name: String,
                        rows: List[Map[String,Option[CellType]]]
                       )

@JsonCodec
case class DictsDataAccum(dicts: List[DictDataRows])

val data: List[DictDataRows] = List(
  DictDataRows("dict1",
    List(
      Map("field1" -> Some(StrType("String val")),
          "field2" -> Some(IntType(123)),
          "field3" -> None),
      Map("field1" -> None,
          "field2" -> None,
          "field3" -> Some(IntType(123)))
    )
  )
)

@JsonCodec
case class RequestResult(status: String,
                         data: DictsDataAccum)

object encodeObjectDataCell {
  implicit val StrTypeEncoder: Encoder[StrType] = new Encoder[StrType] {
    override def apply(v: StrType): Json = Json.fromString(v.s)
  }

  implicit val IntTypeEncoder: Encoder[IntType] = new Encoder[IntType] {
    override def apply(v: IntType): Json = Json.fromInt(v.i)
  }

}

Printer.spaces2.print(RequestResult("ok", DictsDataAccum(data)).asJson)