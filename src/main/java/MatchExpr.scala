object MatchExpr extends App {

  //1) simple String match
  val firstArg = "z"
  val res = firstArg match {
    case "x" => "x--->"
    case "y" => "y----->"
    case _   => "any value"
  }
  println(firstArg+" - "+res)

  //2)

}
