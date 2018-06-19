object MatchExpr extends App {

  //1) simple String match
  val firstArg = "y"
  val res = firstArg match {
    case "x" => "x--->"
    case "y" => "y----->"
    case _   => "any value"
  }
  println(firstArg+" - "+res)

  //

  val f = (_: Int) + (_: Int)
  println(f(3,4))


  def sum(a: Int, b: Int, c: Int) =
    if (!b.isNaN) a + b + c
    else a+c


  def b(f: (Int,Int,Int) => Int, params:(Int,Int,Int)): Int = {
    f(params._1, params._2, params._3)
  }

  println("bRes="+b(sum,(10,20,30)))

  println("================================")

  def mv1f = {
    10
  }

  def mv2f = {
    20
  }

  var mv = 2

  val moreF = mv match {
    case 1 => mv1f
    case 2 => mv2f
  }

  val addMore = (x: Int) => x + moreF

  println("addMore="+addMore(100).toString())

  mv = 1
  println("addMore="+addMore(100).toString())


  case class Person(id:Int,name:String)

  def listPersons(persons : Person*): Unit ={
    println("persons.getClass.getName="+persons.getClass.getName)
    persons.foreach(p => println(p))
  }

  val arr :Seq[Person] = Seq(new Person(1,"Ben"),new Person(2,"john"))

  listPersons(arr: _*)

  def printTime(out: java.io.PrintStream = Console.out) = {
    out.println("time = " + System.currentTimeMillis())
  }

  printTime(Console.err)

}
