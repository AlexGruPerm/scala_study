package bar.calculator

import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object ReadCassandraExamples extends App {
    val logger = LoggerFactory.getLogger(ReadCassandraExamples.getClass)
    logger.info("BEGIN APPLICATION.")

    val client = new SimpleClient("127.0.0.1")
    val barCalc = new BarCalculator(client.session)

    def taskCalcBars(): Future[Unit] = Future {
      barCalc.calc()
      Thread.sleep(10000) // 30000
    }

    def taskBarPatternSearch(): Future[Unit] = Future {
      //barCalc.calc()
      logger.info("=================================================")
      logger.info("....... I am here - taskBarPatternSearch  .......")
      logger.info("=================================================")
      Thread.sleep(5000) // 30000
    }

    def loopCalcBars(): Future[Unit] = {
      taskCalcBars.flatMap(_ => loopCalcBars())
    }

    def looptaskBarPatternSearch(): Future[Unit] = {
      taskBarPatternSearch.flatMap(_ => looptaskBarPatternSearch())
    }

    def infiniteLoop(): Future[Unit] = {
      Future.sequence(List(loopCalcBars())).map(_ => ())
      Future.sequence(List(looptaskBarPatternSearch())).map(_ => ())
    }

    Await.ready(infiniteLoop(), Duration.Inf)

    /*
  val t1 = System.currentTimeMillis
  client.queryTicks
  val t2 = System.currentTimeMillis
  println((t2 - t1) + " msecs")
  */

    client.close
}

