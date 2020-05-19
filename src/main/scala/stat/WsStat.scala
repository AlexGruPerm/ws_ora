package stat

import scala.collection.mutable.ListBuffer

object StatObject {

  case class CacheGetElm(ts: Long, cnt: Int)

  class FixedList[A](max: Int) extends Traversable[A] {

    val list: ListBuffer[A] = ListBuffer()

    def append(elem: A) {
      if (list.size == max) {
        list.trimStart(1)
      }
      list.append(elem)
    }

    def foreach[U](f: A => U) = list.foreach(f)

  }

  case class WsStat(wsStartTs: Long, currGetCnt:Int = 0, statGets: FixedList[CacheGetElm])

}
