import scala.math.Numeric.DoubleIsFractional.div


def isNumInString(s: String) =
  s.replace(".","").replace(",","").replace("-","")
  .forall(_.isDigit)

isNumInString("-1234.0")

