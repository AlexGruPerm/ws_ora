

val s:String = "xxx.yyy"

val arr: Array[String] = s.split("""[.]""")
val (schema,table) = (arr(0),arr(1))

println(schema)
println(table)
