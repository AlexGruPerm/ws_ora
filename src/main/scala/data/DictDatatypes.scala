package data

import akka.util.ByteString
import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import io.circe.syntax._

import scala.collection.immutable.IntMap



@JsonCodec
case class DictRow(name: String, value: String)

/**
 * https://github.com/circe/circe/issues/892
 * if value has null as value, it can produce NPE
 *
 * scala> Foo.modules.logging.logging.x
 * java.lang.NullPointerException
 * at io.circe.Printer$PrintingFolder.onString(Printer.scala:272)
 * at io.circe.Printer$PrintingFolder.onString(Printer.scala:256)
 * at io.circe.Json$JString.foldWith(Json.scala:299)
 * ...
 *
*/

object DictRow {
   def apply(name: String, value: String): DictRow =
    new DictRow(name, if (value==null) "null" else value)
}

@JsonCodec
case class DictDataRows(name: String,
                        connDurMs: Long,
                        execDurMs: Long,
                        fetchDurMs: Long,
                        rows: List[Seq[DictRow]])

@JsonCodec
case class DictsDataAccum(dicts: List[DictDataRows])

@JsonCodec
case class RequestResult(status: String,
                         data: DictsDataAccum)

//todo: maybe add prefix in response, is it from cache or not!?
case class CacheEntity(tscreate: Long, tslru: Long, dictDataRows: DictDataRows, reftables: Seq[String])

/**
 * class for cache entity instance.
 * Summary application cache contains List(CacheEntity)
 * One cache entity ~= one dictionary - DictDataRows
 * setKeys added for optimization, used in CacheManager.get.
 * Before getting value by key _.dictsMap.get(key)
 * we check that key exists in setKeys
*/
case class Cache(
                  HeartbeatCounter: Int,
                  cacheCreatedTs: Long,
                  dictsMap: IntMap[CacheEntity]
                )

object RowType{
  type rows = List[IndexedSeq[DictRow]]
}



