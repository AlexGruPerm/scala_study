package bar

import bar.adviser._
import bar.calculator._
import bar.patternseacher._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object ReadCassandraExamples extends App {
    val logger = LoggerFactory.getLogger(ReadCassandraExamples.getClass)
    logger.info("BEGIN APPLICATION.")

    val client = new bar.calculator.SimpleClient("127.0.0.1")
    val barCalc = new BarCalculator(client.session)
    val patSearch = new PatternSeacher(client.session)
    val tendAdviser = new TendAdviser(client.session)
    val fa = new BarFutureAnalyzer(client.session)

    //val tickersDS = barCalc.getTickersList
    //logger.info("tickersDS.size="+tickersDS.size)

    val adviser = new TradeAdviser(client.session)

    def taskCalcBars(): Future[Unit] = Future {
      val t1 = System.currentTimeMillis
      barCalc.calc()
      val t2 = System.currentTimeMillis
      logger.info("Duration of barCalc.calc() - "+(t2 - t1) + " msecs.")
      Thread.sleep(30000)
    }

    def taskPattSearch(): Future[Unit] = Future {
      val t1 = System.currentTimeMillis
      patSearch.calc()
      val t2 = System.currentTimeMillis
      logger.info("Duration of patSearch.calc() - "+(t2 - t1) + " msecs.")
      Thread.sleep(10000)
    }

  def taskAdviser(): Future[Unit] = Future {
    val t1 = System.currentTimeMillis
    adviser.calc()
    /**
      * Последние 3 бара достаются не правильно, не учитывается партиция по дате, м.б. взята не за последнюю дату!!!
      * надо так
      * select * from val where: Nothing = null ticker_id = 12 val ddate: Nothing = '2018-08-02' val bar_width_sec: Nothing = 600 val by: Nothing = null val desc: Nothing = null
      */
    val t2 = System.currentTimeMillis
    logger.info("Duration of taskAdviser.calc() - "+(t2 - t1) + " msecs.")
    Thread.sleep(10000)
  }

  def taskTendAdviser(): Future[Unit] = Future {
    val t1 = System.currentTimeMillis
    tendAdviser.calc()
    val t2 = System.currentTimeMillis
    logger.info("Duration of tendAdviser.calc() - "+(t2 - t1) + " msecs.")
    Thread.sleep(30000)
  }

    def taskAnyCalc(): Future[Unit] = Future {
      logger.info("=================================================")
      logger.info("....... I am here - taskBarPatternSearch  .......")
      logger.info("=================================================")
      /**
        * Раз в минуту.
        *  Расчет средних и статистических характеристик.
        *  Среднее количество тиков в секунду по каждому тикеру и ширине.
        *  Распределения приращений, за последние N часов и как оно меняется к текущему моменту, поиск общих тенденций.
        */
      Thread.sleep(5000)
    }

  def taskFutAnalyze(): Future[Unit] = Future {
    val t1 = System.currentTimeMillis
    fa.calc()
    val t2 = System.currentTimeMillis
    logger.info("Duration of taskFutAnalyze.calc() - "+(t2 - t1) + " msecs.")
    Thread.sleep(30000)
  }

    def loopCalcBars(): Future[Unit] = {
      taskCalcBars.flatMap(_ => loopCalcBars())
    }

    def loopTaskAnyCalc(): Future[Unit] = {
      taskAnyCalc.flatMap(_ => loopTaskAnyCalc())
    }

  def loopPatSearch(): Future[Unit] = {
    taskPattSearch.flatMap(_ => loopPatSearch())
  }

  def loopAdviser() : Future[Unit] = {
    taskAdviser.flatMap(_ => loopAdviser())
  }

  def loopTendAdviser() : Future[Unit] = {
    taskTendAdviser.flatMap(_ => loopTendAdviser())
  }

  def loopFutAnalyze() : Future[Unit] = {
    taskFutAnalyze.flatMap(_ => loopFutAnalyze())
  }

    def infiniteLoop(): Future[Unit] = {
       Future.sequence(List(loopCalcBars(),loopFutAnalyze())).map(_ => ())

      // Future.sequence(List(loopPatSearch())).map(_ => ())
      // Future.sequence(List(loopTaskAnyCalc())).map(_ => ())

       Future.sequence(List(loopAdviser())).map(_ => ())
       Future.sequence(List(loopTendAdviser())).map(_ => ())
    }

    Await.ready(infiniteLoop(), Duration.Inf)
    client.close
}

