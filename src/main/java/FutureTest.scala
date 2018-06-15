import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.util.{Success, Failure, Try}
import scala.concurrent.ExecutionContext.Implicits.global

/*
*DOCS:
* 1) https://habr.com/post/233555/
* 2) https://viktorklang.com/blog/Futures-in-Scala-2.12-part-5.html
*    http://groz.github.io/scala/intro/futures/
*    https://docs.scala-lang.org/sips/completed/futures-promises.html
*    https://stackoverflow.com/questions/25056957/why-future-sequence-executes-my-futures-in-parallel-rather-than-in-series
*
**/

object FutureTest extends App {
  /*
  def getF(fNum : Int) = {
    val sleepDuration = math.round(math.random()*100)
    Thread.sleep(sleepDuration)
    val f = Future {
      (fNum, 10)
    }
    f
  }

  val seqFuts = for(i <- 2 to 10) yield {
    getF(i).onComplete {
      case Success(sRes: (Int,Int)) => println(" Future:"+sRes._1+" Value:"+sRes._2)
      case Failure(fRes) => println(fRes)
    }
  }
  */


  val successors = Map(1 -> 2, 2 -> 3, 3 -> 4)

  println("successors.get(5)"+successors.get(5))

  successors.get(5) match {
    case Some(n) => println(s"Successor is: $n")
    case None => println("Could not find successor.")
  }

  (1==2) match {
    case true  => println("equal")
    case false => println("not equal")
  }


  //Using match to determine how many arguments take function




  val f1a  = (a1: Int) => a1*2
  val f2a  = (a1: Int, a2: Int) => a1+a2
  val f2ad  = (a1: Int, a2: Int) => a1+a2*0.34

  val f=f2a

  val res = f(1,10)
  println("res = "+res+" withType="+res.getClass.getName)


  val l = f//(f,res)

  /*
  l match {
    case _:Function1[Int,Int] && 1==1 => println("This is a function of [Int] return Int")
  }
  */

/*
x match {
   case _:MyFirst | _:MySecond => doSomething(x) // just use x instead of aOrB
   case _ => doSomethingElse(x)
}
*/

/*
    case _:Function1[Int, Int] =>
    case _:Function2[Int, Int, Int] =>
    case _:Function2[Int, Int, Double] =>
*/

  /*
  f match {
    case _:(Int => Int)       => println("This is a function of 1 argument")
    case _:((Int,Int) => Int) => println("This is a function of 2 arguments")
    case _:AnyRef =>   println("Something else")
  }
  */

  /*
  def f(x: Any): String = x match {
    case i:Int => "integer: " + i
    case _:Double => "a double"
    case s:String => "I want to say " + s
  }
  */


  println(">>>")
  Thread.sleep(1000)
  println("<<<")


}

  //val sumF1 = Future {(slowFunc_1.sum,1)}
  //val sumF = if (v1 > v2) getFuture3 else getFuture4

  //Соответственно,
  // в случае успеха ваша функция обратного вызова onComplete будет вызывана с объектом класса Success
  // , а в случае ошибки - Failure.
  /*
  sumF onComplete {
    //case s => println("  Future:"+s.get._2+" Value:"+s.get._1)
    case Success(result: (Int,Int)) => println("  Future:"+result._2+" Value:"+result._1)
    case Failure(ex: Exception) => println(ex)
  }
  */

  /*
  val result = Future.sequence(Seq(getF1, getF2))

  println(">>>")
  Thread.sleep(1000)
  println("<<<")
  */
