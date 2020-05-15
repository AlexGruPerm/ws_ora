import io.circe._
import io.circe.parser._
import io.circe.generic.auto._

case class Person(name:String, age: Int)

val p: Person = Person("Alice",27)

val pEncode :String = Encoder[Person].apply(p).toString()

val someString :String =
  """{
    |"name" : "Alice",
    |"age"" : 27
    } """.stripMargin

// part 1 - https://www.youtube.com/watch?v=712WE9Ou7BE&t=2321s
// part 2 - https://www.youtube.com/watch?v=OJhAptqtwYo

