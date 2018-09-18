package bar

import bar.calculator.BarFutAnalRes
import com.datastax.driver.core.Session
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters
import scala.collection.JavaConverters._


class PatternSearcher(session: Session) extends PatternSearcherCommon(session, LoggerFactory.getLogger(ReadCassandraExamples.getClass)) {
  val logger = LoggerFactory.getLogger(ReadCassandraExamples.getClass)

  // How much bars in pattern, taken from last
  def calc(deepCntPattern :Int)={
    logger.info("Begin pattern searcher.")

    val tickersProps :Seq[TickerProperty]  = JavaConverters.asScalaIteratorConverter(session.execute(prepTickersWidths.bind()).all().iterator())
      .asScala.toSeq.map(r => new TickerProperty(r.getInt("ticker_id"),
      r.getInt("bar_width_sec"),
      deepCntPattern)
    )
      .sortBy(_.bar_width_sec).toList/*.filter(tp => tp.ticker_id==11 && tp.bar_width_sec==30)*/  //#DEBUG FILTER

    val FullRes = for (tp <- tickersProps) yield {
      //logger.info("   ticker="+tp.ticker_id+" width_sec="+tp.bar_width_sec+"   fullSeqBars="+tp.seqBars.size/*+"   getSearchArea="+tp.searchArea.size*/)

      tp.patternForSearch match {
        case pfs: PatternForSearchCls => {
          logger.info("> Pattern last bar PRICE_C="+pfs.getSeqBarCpat.last.b.c)
          for ((bp, idx) <- pfs.getSeqBarCpat.zipWithIndex) {
            // Temporary closed.
            logger.info(" index=  " + idx + " " + bp.b.btype + "  ticksCnt=" + bp.b.ticks_cnt + " tsBegin="+bp.b.ts_begin+" tsEnd=" + bp.b.ts_end +
              " prcntTicks (" + bp.ticks_cnt_prcnt_from + " - " + bp.ticks_cnt_prcnt_to + ") " +
              " prcntLogCO (" + bp.abs_logco_prcnt_from + " - " + bp.abs_logco_prcnt_to + ")"
            )

          }
        }
        case None => logger.info("patternForSearch is Empty")
      }

      //logger.info(/*"searchArea.size="+tp.searchArea.size+*/"      LAST ts_end="+{if (tp.searchArea.nonEmpty) tp.searchArea.last.ts_end})

      //val searchRes :Seq[BarC] =
      val res = tp.getSearchResult

      //next need analyze FutureSeacher results for making predictions !!!!!!
      logger.info("   ticker="+tp.ticker_id+" width_sec="+tp.bar_width_sec+"   fullSeqBars="+tp.seqBars.size+ " RES = "+res.size+" "+{if (res.size != 0) "  ***  " else " "})

      /* COMBINE ALL ANALYSIS results and input parameters INTO new Class For next saving into db.*/

      /*
      Temporary closed.
       */

      for(r <- res) {
        logger.info(/*"    size="+r.size+*/ "           ts_begin "+r.head.ts_begin+"  ts_end "+r.last.ts_end)
      }


      val lst = res.map(ri => ri.last).map(r => r.ts_end.asInstanceOf[java.lang.Long]).toList

      logger.info("lst.size="+lst.size)

      val resFromFutAnal :Seq[BarFutAnalRes] = tp.getFutAnalResultsByTsEnds(lst.asJava)

      logger.info("resFromFutAnal.size = "+resFromFutAnal.size)

      for (rfa <- resFromFutAnal) {
        logger.info(" FutAnal="+rfa.toString)
      }

      //logger.info("-----------------------------------------------")
      //return for next process
      (
        tp.ticker_id,
        tp.bar_width_sec,
        // CurrPattern
        {tp.patternForSearch match {
          case pfs: PatternForSearchCls => pfs.getSeqBarCpat
          case None => null
        }},
        // FoundCompared
        res,
        // FutAnalResults
        resFromFutAnal
      )
    }

    // compexOutputReults = {
    logger.info("=============================")
    logger.info("OUTPUT FullRes.size="+FullRes.size)
    for (thisRes <- FullRes if thisRes._5.nonEmpty) {
      logger.info("ticker_id="+thisRes._1+" width="+thisRes._2+" currPatternBarsSize="+thisRes._3.size+" FoundPatternsInHistory="+thisRes._4.size+" FoundFutureAnalBars="+thisRes._5.size)

      /*
      ticker_id            int,
        bar_width_sec        int,
          patt_ts_begin        bigint,       // ts_begin первого    бара в искомой (текущей) формации
          patt_ts_end          bigint,       // ts_end   последнего бара в искомой формации
          patt_bars_count      int,          // количество баров в искомой формации
          history_found_tsends list<bigint>, // List(ts_end) найденных формаций в истории по искомой формации
      */

      logger.info("history_found_tsends="+thisRes._4.map(ri => ri.last).map(r => r.ts_end).toString)

      val r_0017_res_u = thisRes._5.filter(rb => rb.ft_log_0017_res=="u").size
      val r_0017_res_d = thisRes._5.filter(rb => rb.ft_log_0017_res=="d").size
      val r_0017_res_n = thisRes._5.filter(rb => rb.ft_log_0017_res=="n").size

      val r_0034_res_u = thisRes._5.filter(rb => rb.ft_log_0034_res=="u").size
      val r_0034_res_d = thisRes._5.filter(rb => rb.ft_log_0034_res=="d").size
      val r_0034_res_n = thisRes._5.filter(rb => rb.ft_log_0034_res=="n").size

      val r_0051_res_u = thisRes._5.filter(rb => rb.ft_log_0051_res=="u").size
      val r_0051_res_d = thisRes._5.filter(rb => rb.ft_log_0051_res=="d").size
      val r_0051_res_n = thisRes._5.filter(rb => rb.ft_log_0051_res=="n").size

      val r_sum_res_u = r_0017_res_u + r_0034_res_u + r_0051_res_u
      val r_sum_res_d = r_0017_res_d + r_0034_res_d + r_0051_res_d
      val r_sum_res_n = r_0017_res_n + r_0034_res_n + r_0051_res_n

      logger.info("r_sum_res_u="+r_sum_res_u+"  r_sum_res_d="+r_sum_res_d+"  r_sum_res_n="+r_sum_res_n)

      val boundSavePattSearch = prepSavePatSearchRes.bind()
        .setInt("p_ticker_id",thisRes._1)
        .setInt("p_bar_width_sec",thisRes._2)
        .setLong("p_patt_ts_begin",thisRes._3.head.b.ts_begin)
        .setLong("p_patt_ts_end",thisRes._3.last.b.ts_end)
        .setDouble("p_patt_end_c",thisRes._3.last.b.c)
        .setInt("p_patt_bars_count",thisRes._3.size)
        .setList("p_history_found_tsends",thisRes._4.map(ri => ri.last).map(r => r.ts_end.asInstanceOf[java.lang.Long]).toList.asJava)
        .setInt("p_ft_log_0017_res_u",r_0017_res_u)
        .setInt("p_ft_log_0017_res_d",r_0017_res_d)
        .setInt("p_ft_log_0017_res_n",r_0017_res_n)
        .setInt("p_ft_log_0034_res_u",r_0034_res_u)
        .setInt("p_ft_log_0034_res_d",r_0034_res_d)
        .setInt("p_ft_log_0034_res_n",r_0034_res_n)
        .setInt("p_ft_log_0051_res_u",r_0051_res_u)
        .setInt("p_ft_log_0051_res_d",r_0051_res_d)
        .setInt("p_ft_log_0051_res_n",r_0051_res_n)
        .setInt("p_ft_log_sum_u",r_sum_res_u)
        .setInt("p_ft_log_sum_d",r_sum_res_d)
        .setInt("p_ft_log_sum_n",r_sum_res_n)

      session.execute(boundSavePattSearch)
    }


    // saveResultsIntoDB


    logger.info("End pattern searcher.")
  }

}
