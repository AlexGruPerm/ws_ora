
val o :Option[Int] = Some(2)

val r:Int = o.fold(-1)(v => v*2)

/*
   Returns true if this option is empty '''or''' the predicate
    $p returns true when applied to this $option's value.
*/
o.forall(nocach==1)