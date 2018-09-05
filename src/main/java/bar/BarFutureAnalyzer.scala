package bar

import bar.calculator.BarC
import com.datastax.driver.core.Session
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters

class BarFutureAnalyzer(session: Session) extends rowToX(session, LoggerFactory.getLogger(ReadCassandraExamples.getClass)){
  val logger = LoggerFactory.getLogger(ReadCassandraExamples.getClass)

  /*
  val tickersWidths  = JavaConverters.asScalaIteratorConverter(session.execute(prepTickersWidths.bind()).all().iterator())
    .asScala.toSeq.map(r => new TickersWidth(r.getInt("ticker_id"),
    r.getInt("bar_width_sec")
  ))
    .sortBy(_.ticker_id).toList
  */

  def simpleRound5Double(valueD : Double) = {
    (valueD * 100000).round / 100000.toDouble
  }

  /**
    * Make search in the Future for 1 fixed log.
    * @param restBars  rest of bars after current
    * @param logReturn : fixed log Return for plua and minus to current bar price (Close)
    * Return : (Double,Char,Long) - Close price, Result type:u,d ., ts_end of close bar.
    */
  def searchFut(currBar : BarC, restBars : Seq[BarC], logReturn : Double) =  {
    val uPrice :Double = simpleRound5Double(Math.exp(Math.log(currBar.c) + logReturn))
    val dPrice :Double = simpleRound5Double(Math.exp(Math.log(currBar.c) - logReturn))
    restBars.find(p => ((uPrice > p.l && uPrice < p.h) || dPrice > p.l && dPrice < p.h)) match {
      case Some(b) => {
        if (b.l > currBar.c) (uPrice,"u",b.ts_end)
        else if (b.h < currBar.c) (dPrice,"d",b.ts_end)
        else (0.toDouble,"n",0.toLong)
      }
      case None => (0.toDouble,"n",0.toLong)
    }
  }

  def analyzeThisBar(currBar :BarC, tickersWidths : Seq[TickersWidth])= {
    /**
      * restBars contains all bars for ticker and width_sec that has ts_begin less than ts_end of current input bar.
      */
    val restBars = tickersWidths.filter(tw => tw.ticker_id == currBar.ticker_id && tw.bar_width_sec == currBar.bar_width_sec)
      .head.getSeqBars
      .dropWhile((b :BarC) => {b.ts_begin < currBar.ts_end})
    val listOfLogs = Seq(0.0017, 0.0034, 0.0051)

    val resOfAnalyze = for(lg <- listOfLogs) yield searchFut(currBar,restBars,lg)

    new BarFutureAnal(
      currBar.ticker_id,
      currBar.bar_width_sec,
      currBar.ts_end,
      currBar.c,
      //-----------------
      resOfAnalyze(0)._3,
      resOfAnalyze(0)._2,
      resOfAnalyze(0)._1,
      //-----------------
      resOfAnalyze(1)._3,
      resOfAnalyze(1)._2,
      resOfAnalyze(1)._1,
      //-----------------
      resOfAnalyze(2)._3,
      resOfAnalyze(2)._2,
      resOfAnalyze(2)._1
    )
  }

  def calc() = {
    logger.info("BarFutureAnalyzer - Begin calc")
    val tickersWidths : Seq[TickersWidth]  = JavaConverters.asScalaIteratorConverter(session.execute(prepTickersWidths.bind()).all().iterator())
      .asScala.toSeq.map(r => new TickersWidth(r.getInt("ticker_id"),
                                               r.getInt("bar_width_sec")
                                              ))
      .sortBy(_.ticker_id).toList

    tickersWidths.foreach(r => logger.info("1. ticker_id = "+r.ticker_id+" width_sec = "+r.bar_width_sec+
      " FROM max_ts_end = "+r.getMaxTsEnd+" READED "+r.getSeqBars.size+" BARS."))

    val seqBarAnalyzed :Seq[BarFutureAnal] =
      for (                    // чтобы было на чём считать, делаем паузу для накопления новых баров.
        tw <- tickersWidths if tw.diffMaxTsEndBarsSeq_MINUS_MaxTsEndFA >= 100*tw.bar_width_sec;
        seqB <- tw.getSeqBars
      )
        yield {
            logger.info("Ticker_id="+tw.ticker_id+" width_sec="+tw.bar_width_sec+" tw.maxTsEnd(FA)="+tw.maxTsEnd+" maxTsEndBars="+tw.maxTsEndBars+" Diff[sec.]="+tw.diffMaxTsEndBarsSeq_MINUS_MaxTsEndFA)
            analyzeThisBar(seqB, tickersWidths)
        }

    logger.info("FULL SIZE ANALYZED BARS = "+seqBarAnalyzed.size)
    for(r <- seqBarAnalyzed.map(sb => sb.ticker_id).distinct) logger.info(" 2. ticker="+r+" bars size="+seqBarAnalyzed.filter(sb => sb.ticker_id==r).size)

    for(r <- seqBarAnalyzed) {
      val boundSaveFutureAnalyze = prepInsertFutureAnalyze.bind()
        .setInt("p_ticker_id",r.ticker_id)
        .setInt("p_bar_width_sec",r.bar_width_sec)
        .setLong("p_ts_end",r.ts_end)
        .setDouble("p_c",r.c)
        //-----------
        .setDouble("p_ft_log_0017_cls_price",r.ft_log_0017_cls_price)
        .setString("p_ft_log_0017_res",r.ft_log_0017_res)
        .setLong("p_ft_log_0017_ts_end",r.ft_log_0017_ts_end)
        //-----------
        .setDouble("p_ft_log_0034_cls_price",r.ft_log_0034_cls_price)
        .setString("p_ft_log_0034_res",r.ft_log_0034_res)
        .setLong("p_ft_log_0034_ts_end",r.ft_log_0034_ts_end)
        //-----------
        .setDouble("p_ft_log_0051_cls_price",r.ft_log_0051_cls_price)
        .setString("p_ft_log_0051_res",r.ft_log_0051_res)
        .setLong("p_ft_log_0051_ts_end",r.ft_log_0051_ts_end)
      session.execute(boundSaveFutureAnalyze)
    }

  }

}