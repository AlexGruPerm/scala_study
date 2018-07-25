package bar.calculator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object ReadCassandraExamples extends App {
  println("hello ")
  val client = new SimpleClient("127.0.0.1")

  //client.createSchema
  //client.loadData

  //client.querySchema1
  //client.querySchema2
  //client.querySchema3
  /*
  val t1 = System.currentTimeMillis
  client.queryTicks
  val t2 = System.currentTimeMillis
  println((t2 - t1) + " msecs")
  */

  /*
  val t1 = System.currentTimeMillis
  val ticksData = client.getTicks()
  val t2 = System.currentTimeMillis
  println("ticksData.size="+ticksData.size+" duration = "+(t2 - t1)+ " msecs")
  val sumAsk = ticksData.map(elm => elm.ask).sum
  println("sumAsk="+sumAsk)
*/


  //!!!!!!!!!!!!!
  val barCalc = new BarCalculator(client.session)

  def task1(): Future[Unit] = Future {
    barCalc.calc()
    Thread.sleep(30000)
  }

  def loopTask1(): Future[Unit] = {
    task1.flatMap(_ => loopTask1())
  }

  def infiniteLoop(): Future[Unit] = {
    Future.sequence(List(loopTask1()/*, loopTask2()*/)).map(_ => ())
  }

  Await.ready(infiniteLoop(), Duration.Inf)

  //val t1 = System.currentTimeMillis
  //client.getListTS()
  //val t2 = System.currentTimeMillis


  client.close
}
