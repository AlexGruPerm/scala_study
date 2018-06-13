import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

object FutureTest extends App {
  println("App")



/*
  def slowFunc_2(toVal : Int) = {
    Thread.sleep(600)
    for (i <- 11 to toVal) yield i
  }
*/

  def getFuture3 = {
    Thread.sleep(700)
    Future {
      ((for (i <- 21 to 30) yield i).sum,3)
    }
  }

  def getFuture4 = {
    Thread.sleep(900)
    Future {
      ((for (i <- 31 to 40) yield i).sum,4)
    }
  }

  def slowFunc_1 = {
    Thread.sleep(1800)
    for (i <- 1 to 10) yield i
  }



  /*
  val sumF2 = Future {
    slowFunc_2(200).sum // set external data into Future
  }
*/
  val v1 = math.round(math.random()*10)
  val v2 = math.round(math.random()*10)

  println(v1+" - "+v2)


  val sumF1 = Future {(slowFunc_1.sum,1)}
  val sumF = if (v1 > v2) getFuture3 else getFuture4

  /*
  sumF1 onComplete  {
    case s => println("(1)"+s)
  }

  sumF2 onComplete  {
    case s => println("(2)"+s)
  }
  */

  //Соответственно,
  // в случае успеха ваша функция обратного вызова будет вызывана с объектом класса Success
  // , а в случае ошибки - Failure.
  sumF onComplete {
    //case s => println("  Future:"+s.get._2+" Value:"+s.get._1)
    case Success(result: (Int,Int)) => println("  Future:"+result._2+" Value:"+result._1)
    case Failure(ex: Exception) => println(ex)
  }

  sumF1 onComplete {
    //case s => println("  Future:"+s.get._2+" Value:"+s.get._1)
    case Success(result: (Int,Int)) => println("  Future:"+result._2+" Value:"+result._1)
    case Failure(ex: Exception) => println(ex)
  }

 // sumF.foreach(x => println(x))

  val result = Future.sequence(Seq(sumF, sumF1))

  println(">>>")
  Thread.sleep(3000)
  println("<<<")

  /*
  *DOCS:
  * 1) https://habr.com/post/233555/
  * 2) https://viktorklang.com/blog/Futures-in-Scala-2.12-part-5.html
  * http://groz.github.io/scala/intro/futures/
  * https://docs.scala-lang.org/sips/completed/futures-promises.html
  * https://stackoverflow.com/questions/25056957/why-future-sequence-executes-my-futures-in-parallel-rather-than-in-series
  *
  **/




}
