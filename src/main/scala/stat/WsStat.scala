package stat

import scala.collection.mutable.ListBuffer

object StatObject {

  case class CacheGetElm(ts: Long, cnt: Int)

  class FixedList[CacheGetElm](max: Int) extends Traversable[CacheGetElm] {

    val list: ListBuffer[CacheGetElm] = ListBuffer()

    def append(elem: CacheGetElm) {
      if (list.size == max) {
        list.trimStart(1)
      }
      list.append(elem)
    }

    def foreach[U](f: CacheGetElm => U) = list.foreach(f)

  }

  case class WsStat(wsStartTs: Long, currGetCnt:Int = 0, statGets: FixedList[CacheGetElm])

}
