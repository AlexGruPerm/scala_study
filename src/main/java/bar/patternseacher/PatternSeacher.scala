package bar.patternseacher

import bar.{ReadCassandraExamples, rowToX}
import bar.calculator._
import bar.calculator.BarCalculator
import java.text.SimpleDateFormat
import java.util.Date

import com.datastax.driver.core
import com.datastax.driver.core.{LocalDate, Row, Session}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters

class PatternSeacher(session: Session) extends rowToX(session, LoggerFactory.getLogger(ReadCassandraExamples.getClass)) {

  val logger = LoggerFactory.getLogger(ReadCassandraExamples.getClass)

  /**
    * Reas table bars_property_last_deeps and return how many bars keep for each ticker+width
    */
  def get_bars_last_deeps() = {
    val rsList = session.execute("""select
                                          ticker_id,
                                          bar_width_sec,
                                          deep_count
                                     from mts_meta.bars_property_last_deeps
                                    where is_enabled = 1
                                          allow filtering; """).all()
    val res = for(i <- 0 to rsList.size()-1) yield {
      new bar_last_deep(
        rsList.get(i).getInt("ticker_id"),
        rsList.get(i).getInt("bar_width_sec"),
        rsList.get(i).getInt("deep_count")
      )
    }
    res
  }

  /**
    * Main function for all calculation and operations.
    * 1) Get properties for deeps (count of last bars)
    *    Get last N bars from mts_bars.bars by ticker_id, width_sec.
    *    Save into table mts_bars.lastNbars with curr_ts from last Bar end_ts.
    * 2) Read all bars from history and make pattern search with current last Bars sequence.
    * 3) Save results of search into XXX wide table.
    *
    */
  def calc()  = {
    logger.info("BEGIN PatternSeacher calc()")

    val barsNLast_Property : Seq[bar_last_deep] = get_bars_last_deeps()
    for (oneBarWdthLDeepProp <- barsNLast_Property) {
      logger.info("oneBarWdthLDeepProp : ticker_id=" + oneBarWdthLDeepProp.ticker_id + " bar_width_sec=" + oneBarWdthLDeepProp.bar_width_sec + " deep_count="+oneBarWdthLDeepProp.deep_count)
      val bound = prepGetLastNBars.bind().setInt("p_ticker_id", oneBarWdthLDeepProp.ticker_id)
                                         .setInt("p_bar_width_sec", oneBarWdthLDeepProp.bar_width_sec)
                                         .setInt("p_limit", oneBarWdthLDeepProp.deep_count)

      val rsLBars : Seq[BarC] = JavaConverters.asScalaIteratorConverter(session.execute(bound).all().iterator())
        .asScala
        .toSeq.map(row =>
        new BarC(
          row.getInt("ticker_id"),
          new Date(row.getDate("ddate").getMillisSinceEpoch),
          row.getInt("bar_width_sec"),
          row.getLong("ts_begin"),
          row.getLong("ts_end"),
          row.getDouble("o"),
          row.getDouble("h"),
          row.getDouble("l"),
          row.getDouble("c"),
          row.getDouble("h_body"),
          row.getDouble("h_shad"),
          row.getString("btype"),
          row.getLong("ts_end_unx"),
          row.getInt("ticks_cnt"),
          row.getDouble("disp"),
          row.getDouble("log_co")
        )
      )
      //head - last bar in history, fresh
      //last - older
      logger.info("rsLBars.size="+rsLBars.size+" FIRST(most fresh)="+ rsLBars.head.ts_end_unx +" OLDER(back in history)="+rsLBars.last.ts_end_unx)
      logger.info("Next it will use for pattern search and save result for ticker, width_sec, deep, ts_begin (first bar), ts_end (last bar)  ")
      /*
      if (rsLBars.nonEmpty && rsLBars.size == oneBarWdthLDeepProp.deep_count) {
        val lastBarTsEnd = rsLBars.head.ts_end
        for (oneBar <- rsLBars) {
          val boundSaveOneOfLastBAR = prepOneOfLastBars.bind()
            .setTimestamp("p_last_bar_ts_end", lastBarTsEnd)
            .setInt("p_deep", oneBarWdthLDeepProp.deep_count)
            .setInt("p_ticker_id", oneBarWdthLDeepProp.ticker_id)
            .setInt("p_bar_width_sec", oneBar.bar_width_sec)
            .setTimestamp("p_ts_begin", oneBar.ts_begin)
            .setTimestamp("p_ts_end", oneBar.ts_end)
            .setDouble("p_o", oneBar.o)
            .setDouble("p_h", oneBar.h)
            .setDouble("p_l", oneBar.l)
            .setDouble("p_c", oneBar.c)
            .setDouble("p_h_body", oneBar.h_body)
            .setDouble("p_h_shad", oneBar.h_shad)
            .setString("p_btype", oneBar.btype)
            .setInt("p_ticks_cnt", oneBar.ticks_cnt)
            .setDouble("p_disp", oneBar.disp)
          session.execute(boundSaveOneOfLastBAR)
        }
        logger.info("    SAVED " + rsLBars.size + " LAST BARS INTO mts_bars.lastNbars for ticker=[" + oneBarWdthLDeepProp.ticker_id)
      }
      */

    }


    //save last basr by deep for this Ticker - Width
    /*
    val lbarsDeep = barsNLast_Property.filter(blp => blp.ticker_id == ticker.ticker_id && blp.bar_width_sec == bp.bar_width_sec)
     if (lbarsDeep.nonEmpty && seqBarsCalced.size >= lbarsDeep.head.deep_count) {
       // if lbarsDeep non empty it contains only one row
       // (beacuse PK - ticker_id, bar_width_sec, is_enabled, deep_count) filtered outside is_enabled=1

       //Clear all before
       val prepClear = prepClearLastNBars.bind()
                               .setInt("p_deep",lbarsDeep.head.deep_count)
                               .setInt("p_ticker_id",ticker.ticker_id)
                               .setInt("p_bar_width_sec",bp.bar_width_sec)
       session.execute(prepClear)

       for (oneBar <- seqBarsCalced.takeRight(lbarsDeep.head.deep_count)){
         val boundSaveOneOfLastBAR = prepOneOfLastBars.bind()
           .setInt("p_deep",lbarsDeep.head.deep_count)
           .setInt("p_ticker_id", ticker.ticker_id)
           .setInt("p_bar_width_sec", oneBar.bar_width_sec)
           .setTimestamp("p_ts_begin", oneBar.ts_begin)
           .setTimestamp("p_ts_end", oneBar.ts_end)
           .setDouble("p_o", oneBar.o)
           .setDouble("p_h", oneBar.h)
           .setDouble("p_l", oneBar.l)
           .setDouble("p_c", oneBar.c)
           .setDouble("p_h_body", oneBar.h_body)
           .setDouble("p_h_shad", oneBar.h_shad)
           .setString("p_btype", oneBar.btype)
           .setInt("p_ticks_cnt", oneBar.ticks_cnt)
           .setDouble("p_disp", oneBar.disp)
         session.execute(boundSaveOneOfLastBAR)
       }
       logger.info("    SAVED "+lbarsDeep.head.deep_count+" LAST BARS INTO mts_bars.lastNbars for ticker=["+ticker.ticker_id+"] wdt=["+bp.bar_width_sec+"]")
     }
    */

  }

}
