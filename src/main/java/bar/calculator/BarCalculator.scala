package bar.calculator

import java.text.SimpleDateFormat
import java.util.Date

import com.datastax.driver.core
import com.datastax.driver.core.{LocalDate, Row, Session}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters

case class bars_property(ticker_id      :Int,
                         bar_width_sec  :Int,
                         is_enabled     :Int)

case class 	BarC(
                 ticker_id       :Int,
                 ddate           :java.util.Date,
                 bar_width_sec   :Int,
                 ts_begin        :java.util.Date,
                 ts_end          :java.util.Date,
                 o               :Double,
                 h               :Double,
                 l               :Double,
                 c               :Double,
                 h_body          :Double,
                 h_shad          :Double,
                 btype           :String,
                 ts_end_unx      :Long
                )


case class LastBars(seqBars : Seq[BarC]) {
  def size : Int = seqBars.size

  val logger = LoggerFactory.getLogger(ReadCassandraExamples.getClass)

  logger.debug("Constructor of LastBars size="+size)

  //if(i < 0)
  //  throw new IllegalArgumentException("the number must be non-negative.")

}



case class bar_last_deep(
                          ticker_id     :Int,
                          bar_width_sec :Int,
                          deep_count    :Int
                        )


case class Ticker(
                   ticker_id        :Int,
                   ticker_code      :String,
                   last_tick_ddate  :java.util.Date,
                   last_tick_ts     :java.util.Date,  // Max ts from mts_src.ticks for this ticker.
                   last_tick_ts_unx :Long             // Max ts
                 )

case class pair_ts_tsunx(
                          ts      :java.util.Date,
                          ts_unx  :Long
                        )

case class barProps(
                     ticker_id     :Int,
                     ddate         :LocalDate,
                     bar_width_sec :Int
                   )

/**
  *
  * case class for return from calc/save bars function.
  * ticker_id            - which ticker was processed
  * prev_last_bar_ts_unx - value of Ticker.last_bar_ts_unx when calculation begin.
  * curr_last_bar_ts_unx - saved last bar tsunx.
  * saved_bars_count     - how many bars ware saved into db.
  *
  */
case class ticker_bars_save_result(
                                    ticker_id            :Int,
                                    bar_width_sec        :Int,
                                    prev_last_bar_ts_unx :Long,
                                    curr_last_bar_ts_unx :Long,
                                    saved_bars_count     :Int
                                  )

/**
  * Class for calculate bars in background.
  * 1. Read meta information from table mts_meta.bars_property
  * 2. Read last bars ware calculated from mts_bars.bars
  * 3. Analyze source ticks for new bars and calculate it
  */
class BarCalculator(session: Session) {

  val logger = LoggerFactory.getLogger(ReadCassandraExamples.getClass)

  val resLastDateBar = session.prepare("""select max(ddate) as ddate
                                              from mts_bars.bars
                                             where ticker_id     = :tickerId and
                                                   bar_width_sec = :barWidth
                                            ALLOW FILTERING; """);

  val prepared = session.prepare(""" select max(ts) as ts,
                                              toUnixTimestamp(max(ts)) as tsunx
                                       from mts_src.ticks
                                      where ticker_id = :tickerId and
                                            ddate     = :maxDDate
                                   """)

  val resLastBarPrep = session.prepare(
    """
               select
              	  ticker_id,
              	  ddate,
              	  bar_width_sec,
                  ts_begin,
                  ts_end,
                  o,
                  h,
                  l,
                  c,
                  h_body,
                  h_shad,
                  btype,
                  toUnixTimestamp(max(ts_end)) as ts_end_unx
               from mts_bars.bars
               where ticker_id     = :tickerId and
                     ddate         = :maxDdate and
                     bar_width_sec = :barWidth;
            """)

  val resTicksByTsInterval = session.prepare(
    """select ticker_id,
        	      ddate,
                ts,
                bid,
                ask
           from mts_src.ticks
          where ticker_id = :tickerId and
                ts > :ts_begin and
                ts < :ts_end
                 ALLOW FILTERING; """)

  val prepSaveBar = session.prepare(
    """
        insert into mts_bars.bars(
        	  ticker_id,
        	  ddate,
        	  bar_width_sec,
            ts_begin,
            ts_end,
            o,
            h,
            l,
            c,
            h_body,
            h_shad,
            btype,
            ticks_cnt,
            disp
            )
        values(
        	  :p_ticker_id,
        	  :p_ddate,
        	  :p_bar_width_sec,
            :p_ts_begin,
            :p_ts_end,
            :p_o,
            :p_h,
            :p_l,
            :p_c,
            :p_h_body,
            :p_h_shad,
            :p_btype,
            :p_ticks_cnt,
            :p_disp
            ); """)

  val prepSaveOnlineLastBars = session.prepare(
    """
        insert into mts_bars.lastbars(
        	  ticker_id,
        	  bar_width_sec,
            ts_begin,
            ts_end,
            o,
            h,
            l,
            c,
            h_body,
            h_shad,
            btype,
            ticks_cnt,
            disp
            )
        values(
        	  :p_ticker_id,
        	  :p_bar_width_sec,
            :p_ts_begin,
            :p_ts_end,
            :p_o,
            :p_h,
            :p_l,
            :p_c,
            :p_h_body,
            :p_h_shad,
            :p_btype,
            :p_ticks_cnt,
            :p_disp
            ); """)


  val prepOneOfLastBars = session.prepare(
    """insert into mts_bars.lastNbars(
            deep,
        	  ticker_id,
        	  bar_width_sec,
            ts_begin,
            ts_end,
            o,
            h,
            l,
            c,
            h_body,
            h_shad,
            btype,
            ticks_cnt,
            disp
            )
        values(
            :p_deep,
        	  :p_ticker_id,
        	  :p_bar_width_sec,
            :p_ts_begin,
            :p_ts_end,
            :p_o,
            :p_h,
            :p_l,
            :p_c,
            :p_h_body,
            :p_h_shad,
            :p_btype,
            :p_ticks_cnt,
            :p_disp
            ); """)

  val prepClearLastNBars = session.prepare("""delete from mts_bars.lastNbars where deep=:p_deep and ticker_id=:p_ticker_id and bar_width_sec=:p_bar_width_sec; """)

  val rowToBarProperty = (row: Row) => {
    //if (row.getInt("is_enabled") == 1)
      new bars_property(row.getInt("ticker_id"), row.getInt("bar_width_sec"), row.getInt("is_enabled"))
  }

  val rowToBar = (row : Row) => {
    new BarC(
      row.getInt("ticker_id"),
      new Date(row.getDate("ddate").getMillisSinceEpoch),
      row.getInt("bar_width_sec"),
      row.getTimestamp("ts_begin"),
      row.getTimestamp("ts_end"),
      row.getDouble("o"),
      row.getDouble("h"),
      row.getDouble("l"),
      row.getDouble("c"),
      row.getDouble("h_body"),
      row.getDouble("h_shad"),
      row.getString("btype"),
      row.getLong("ts_end_unx")
    )
  }


  /**
    * Return max(ddate) by ticker
    */
  def getLastTickDdate(p_ticker : Int) ={
    val rsList = session.execute("select distinct ddate,ticker_id FROM mts_src.ticks;").all()
    val res = for(i <- 0 to rsList.size()-1) yield
      (
        rsList.get(i).getInt("ticker_id"),
        rsList.get(i).getDate("ddate"),
        new Date(rsList.get(i).getDate("ddate").getMillisSinceEpoch)
      )
    val getLastTickDdate_Res_Max = {if (res.nonEmpty) res.map(x => x._3).max else 0 }
    val max_dates = res.find(x => x._3 == getLastTickDdate_Res_Max &&  x._1 == p_ticker).map(x => (x._2,x._3))
    logger.debug("[getLastTickDdate] p_ticker="+p_ticker+"  max_dates=["+max_dates+"]")
    max_dates

  }


  /**
    * Return max(ts),max(ts) as UNixTimeStamp by ticker
    */
  def getLastTickTs(p_ticker : Int, p_maxdate : LocalDate) = {
    val bound = prepared.bind().setInt("tickerId", p_ticker).setDate("maxDDate",p_maxdate)
    val rsList = session.execute(bound).all()
    val res = for(i <- 0 to rsList.size()-1) yield new pair_ts_tsunx(
                                                                     rsList.get(i).getTimestamp("ts"),
                                                                     rsList.get(i).getLong("tsunx")
                                                                    )
    res(0)
  }


  val rowToTicker = (row : Row) => {
    val dd = getLastTickDdate(row.getInt("ticker_id"))

    dd match {
      case Some(s) => {

        val l_ticker_id = row.getInt("ticker_id")
        val last_tick_ddate = s._2
        val last_tisk_tss : pair_ts_tsunx = getLastTickTs(l_ticker_id/*row.getInt("ticker_id")*/,s._1)
        new Ticker(
          ticker_id        = l_ticker_id,
          ticker_code      = row.getString("ticker_code"),
          last_tick_ddate  = last_tick_ddate,
          last_tick_ts     = last_tisk_tss.ts,
          last_tick_ts_unx = last_tisk_tss.ts_unx
        )

      }
      case None =>
        new Ticker(
        ticker_id        = 0,
        ticker_code      = " ",
        last_tick_ddate  = null,
        last_tick_ts     = null,
        last_tick_ts_unx = 0
      )
    }
  }


  /**
    * Read mts_meta.bars_property and return sequence of objects bars_property only with is_enabled=1
    */
  def get_bars_property() ={
    val results = session.execute("select * from mts_meta.bars_property;")
    val rsList = results.all()
    val bp = for(i <- 0 to rsList.size()-1) yield rowToBarProperty(rsList.get(i))//.asInstanceOf[bars_property]
    bp.filter(b => b.is_enabled==1)
  }

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


  def get_Bars(bars_properties : Seq[bars_property]) = {
    //preparation once, outside next for loop

    val seqOfSeqBarProp: Seq[barProps] =
      for (bp <- bars_properties) yield {
        val boundLastDate = resLastDateBar.bind().setInt("tickerId", bp.ticker_id)
                                                 .setInt("barWidth", bp.bar_width_sec)
        val rsLastDate = session.execute(boundLastDate).one()
          new barProps(bp.ticker_id, rsLastDate.getDate("ddate"), bp.bar_width_sec)
    }

    val seqOfSeqBarPropNN = seqOfSeqBarProp.filter(bp => bp.ddate!=null)
    for (oneBarProp <- seqOfSeqBarPropNN) yield {
      //logger.debug("inside for (oneBarProp <- seqOfSeqBarProp) ticker_id=" + oneBarProp.ticker_id + " ddate=" + oneBarProp.ddate + " bar_width_sec=" + oneBarProp.bar_width_sec)
      val bound = resLastBarPrep.bind().setInt("tickerId", oneBarProp.ticker_id)
                                       .setDate("maxDdate", oneBarProp.ddate)
                                       .setInt("barWidth", oneBarProp.bar_width_sec)
      val rsBar = session.execute(bound).one()
      //logger.debug("___ inside for (oneBarProp <- seqOfSeqBarProp) ticker_id="+rsBar.getInt("ticker_id")+" ts_end_unx="+rsBar.getLong("ts_end_unx"))
      rowToBar(rsBar)
    }

  }


  /**
    * Return seq of Ticker with additional information about timestamps.
    */
  def get_Tickers(ds_tikers_ddates : java.util.List[Row]) = {
    val listTicker : Seq[Ticker] = for(i <- 0 to ds_tikers_ddates.size-1) yield
        rowToTicker(ds_tikers_ddates.get(i))
    listTicker
  }


  /**
    * Function for elimination multiple calls same query.
    * @return
    */
  def get_ds_tickers_ddates() = {
    val results = session.execute("select * from mts_meta.tickers;")
    results.all()
  }



  /** ===============================================================================================================
    *  bars must contain only one Last bars(no one) by this ticker (for each widths) if this exists in db.
    * @param ticker
    * @param bars - Last bars by each width.
    * @return
    */
  def calc_one_ticker(ticker : Ticker, bars : Seq[BarC], barsPropsTicker : Seq[bars_property], barsNLast_Property : Seq[bar_last_deep]) : Seq[ticker_bars_save_result] ={
    logger.info("  2.[calc_one_ticker] ticker=" + ticker.ticker_id)

    val seqMinsBarUnxtsEnd = for (bp <- barsPropsTicker) yield
      bars.filter(b => b.bar_width_sec == bp.bar_width_sec).map(b => b.ts_end_unx).reduceOption(_ min _).getOrElse(0.toLong)

    //logger.debug("   2.1 [calc_one_ticker] seqMinsBarUnxtsEnd.size="+seqMinsBarUnxtsEnd.size)
    val min_unxts_in_bars = {if (barsPropsTicker.size==0)
                               0.toLong
                             else
                             seqMinsBarUnxtsEnd.min}
    //logger.debug("   2.2 [calc_one_ticker] min_unxts_in_bars="+min_unxts_in_bars)
    //logger.debug(" BEFORE session.prepare resTicksByTsInterval min_unxts_in_bars=" + min_unxts_in_bars + " ticker.last_tick_ts_unx="+ticker.last_tick_ts_unx)

    val bound = resTicksByTsInterval.bind().setInt("tickerId", ticker.ticker_id)
                                           .setTimestamp("ts_begin",  new Date(min_unxts_in_bars))
                                           .setTimestamp("ts_end",  new Date(ticker.last_tick_ts_unx))

    val rsTicks : Seq[FinTick] = JavaConverters.asScalaIteratorConverter(session.execute(bound).all().iterator())
                                               .asScala
                                               .toSeq.map(tick => new FinTick(tick.getTimestamp("ts"),
                                                                              tick.getDouble("ask"),
                                                                              tick.getDouble("bid")))
                                               .sortBy(ft => ft.ts)

    if (rsTicks.nonEmpty) {
      logger.debug("      tick 1: " + rsTicks.head.ts.getTime)
      logger.debug("      tick 2: " + rsTicks.tail.head.ts.getTime)
      logger.debug("      tick 3: " + rsTicks.tail.tail.head.ts.getTime)
      logger.info("    READED FROM DB " + rsTicks.size + " TICKS.")
    } else {
      logger.info("    READED FROM DB - rsTicks Empty.")
    }

    val res = for (bp <- barsPropsTicker if rsTicks.nonEmpty) yield {

       logger.debug("    3. bar_width_sec = " + bp.bar_width_sec)
       val lastBar_ByWidth = bars.filter(b => b.bar_width_sec == bp.bar_width_sec)
       val lastBar_ts_end_unx : Long = if (lastBar_ByWidth.isEmpty) 0
                                        else lastBar_ByWidth.head.ts_end_unx

       val lastBat_tsendunx_Diff_ticker_lstunx_Sec = (ticker.last_tick_ts_unx - lastBar_ts_end_unx )/1000

       val dataWidthSec = (ticker.last_tick_ts_unx - Math.max(lastBar_ts_end_unx,rsTicks.head.ts.getTime) )/1000

       logger.debug("       4.   rsTicks.head.ts.getTime="+rsTicks.head.ts.getTime+" lastBar_ts_end_unx="+lastBar_ts_end_unx+"  dataWidthSec=" + dataWidthSec)

       //logger.debug("       4.  lastBar_ts_end_unx = " + lastBar_ts_end_unx +"  Diff[seconds] = " + lastBat_tsendunx_Diff_ticker_lstunx_Sec)
      //Value lastBat_tsendunx_Diff_ticker_lstunx_Sec can be extremaly BIG because lastBar_ts_end_unx can be equal 0.
       if (dataWidthSec/*lastBat_tsendunx_Diff_ticker_lstunx_Sec*/ > bp.bar_width_sec) {
         logger.debug("        5. diff more then width. Start calculation for ticker, width, ts in ticks from "+ min_unxts_in_bars +" to "+ticker.last_tick_ts_unx+" min tsunx_from_bars="+min_unxts_in_bars)

         //OLD BARS BY TICK, need seconds.
         // val seqSeqTicks : Seq[Seq[FinTick]] = rsTicks.sliding(bp.bar_width_sec,bp.bar_width_sec).filter(x => (x.size==bp.bar_width_sec)).toSeq
         //val fIdx = rsTicks.head.ts.getTime
         //val lIdx = rsTicks.last.ts.getTime
         val barsSides = rsTicks.head.ts.getTime.to(rsTicks.last.ts.getTime).by(bp.bar_width_sec*1000)

         val seqBarSides = for ((bs,idx) <- barsSides.zipWithIndex) yield (bs,idx.toInt)

         val seqBar2Sides = for(i <- 0 to seqBarSides.size-1) yield {
           if (i < seqBarSides.last._2)
             (seqBarSides(i)._1, seqBarSides(i+1)._1, seqBarSides(i)._2+1)
           else
             (seqBarSides(i)._1, seqBarSides(i)._1 /*+ bp.bar_width_sec*1000*/, seqBarSides(i)._2+1)  //#########
         }

         def getGroupThisElement(elm : Long)={
           seqBar2Sides.find(bs => (bs._1 <= elm && bs._2 > elm) && (bs._2 - bs._1)/1000 == bp.bar_width_sec).map(x => x._3).getOrElse(0)
         }

         val seqSeqTicks = rsTicks.groupBy(elm => getGroupThisElement(elm.ts.getTime)).filter(seqT => seqT._1!=0 ).toSeq.sortBy(gr => gr._1)
                                                                //Not last group where can be less then bar_width_sec
         val seqBarsCalced = for (seqTicksOneBar <- seqSeqTicks if seqTicksOneBar._1 != 0 ) yield {
           logger.debug("          6. GROUP ID - seqTicksOneBar._1 = "+seqTicksOneBar._1)
           new Bar(
             p_ticker_id = ticker.ticker_id,
             p_bar_width_sec = bp.bar_width_sec,
             barTicks = seqTicksOneBar._2
           )
         }

         logger.info("    CALCULATED BARS "+ seqBarsCalced.size+"  ... may be not full size.")

         if (seqBarsCalced.nonEmpty) {
         //SAVE BARS
         for (b <- seqBarsCalced) {
           val boundSaveBar = prepSaveBar.bind()
             .setInt("p_ticker_id", ticker.ticker_id)
             .setDate("p_ddate",  core.LocalDate.fromMillisSinceEpoch( b.ddate.getTime()))
             .setInt("p_bar_width_sec",b.bar_width_sec)
             .setTimestamp("p_ts_begin", b.ts_begin)
             .setTimestamp("p_ts_end", b.ts_end)
             .setDouble("p_o",b.o)
             .setDouble("p_h",b.h)
             .setDouble("p_l",b.l)
             .setDouble("p_c",b.c)
             .setDouble("p_h_body",b.h_body)
             .setDouble("p_h_shad",b.h_shad)
             .setString("p_btype",b.btype)
             .setInt("p_ticks_cnt",b.ticks_cnt)
             .setDouble("p_disp",b.disp)
           session.execute(boundSaveBar)
         }

         //UPSERT LAST BAR FROM CALCED BARS seqBarsCalced
           val lastBarFromBars = seqBarsCalced.filter(b => b.ts_end_unx == seqBarsCalced.map(bfs => bfs.ts_end_unx).max).head //only one !?

           val boundSaveBarLast = prepSaveOnlineLastBars.bind()
             .setInt("p_ticker_id", ticker.ticker_id)
             .setInt("p_bar_width_sec", lastBarFromBars.bar_width_sec)
             .setTimestamp("p_ts_begin", lastBarFromBars.ts_begin)
             .setTimestamp("p_ts_end", lastBarFromBars.ts_end)
             .setDouble("p_o", lastBarFromBars.o)
             .setDouble("p_h", lastBarFromBars.h)
             .setDouble("p_l", lastBarFromBars.l)
             .setDouble("p_c", lastBarFromBars.c)
             .setDouble("p_h_body", lastBarFromBars.h_body)
             .setDouble("p_h_shad", lastBarFromBars.h_shad)
             .setString("p_btype", lastBarFromBars.btype)
             .setInt("p_ticks_cnt", lastBarFromBars.ticks_cnt)
             .setDouble("p_disp", lastBarFromBars.disp)
           session.execute(boundSaveBarLast)

          //save last basr by deep for this Ticker - Width
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

         }

         logger.info("                ")
         logger.debug("               ")

          new ticker_bars_save_result(
            ticker_id            = ticker.ticker_id,
            bar_width_sec        = bp.bar_width_sec,
            prev_last_bar_ts_unx = lastBar_ts_end_unx,
            curr_last_bar_ts_unx = if (seqBarsCalced.nonEmpty) seqBarsCalced.map(b => b.ts_end_unx).max else 0,
            saved_bars_count     = seqBarsCalced.size
          )
        } else {
         new ticker_bars_save_result(
           ticker_id            = ticker.ticker_id,
           bar_width_sec        = bp.bar_width_sec,
           prev_last_bar_ts_unx = 0,
           curr_last_bar_ts_unx = 0,
           saved_bars_count     = 0
         )
       }
     }



/* ticket -> ticker RENAME
    val thisTicketLastBar = bars

    logger.debug("   3. b.bar_width_sec="+thisTicketLastBar.bar_width_sec +
            " b.ts_end_unx = " + thisTicketLastBar.ts_end_unx+
            " diff_last_bar_unxts_last_tick = "+ (ticker.last_tick_ts_unx - thisTicketLastBar.ts_end_unx)/1000 +
           " seconds")

    if ((ticker.last_tick_ts_unx - thisTicketLastBar.ts_end_unx)/1000 > thisTicketLastBar.bar_width_sec) {
      logger.debug("         --- "+(ticker.last_tick_ts_unx -thisTicketLastBar.ts_end_unx)/1000 +">"+ thisTicketLastBar.bar_width_sec+" CALCULATE THIS WIDTH_TICKER PAIR")
      //check new ticks data for bars calculation
    }
*/

    // так мы пропускаем тикеты по которым вообще нет баров из расчета баров для них !!!
    //
    //  Наоборот поменять чтобы старт был с тикара! и уже дальше смотри есть по нему какие-то посчитанные бары или нет!
    //
    logger.debug(" ----------------------------------------------------------------------")
    res
    /*
    new ticker_bars_save_result(
                                ticker_id            = ticker.ticker_id,
                                bar_width_sec        = 30,
                                prev_last_bar_ts_unx = 1,
                                curr_last_bar_ts_unx = 2,
                                saved_bars_count     = 100
                               )
    */
  }





  /** ===============================================================================================================
    * Make main bar calculations with Futures.
    * @param tickers
    */
  def run_background_calcs(tickers : Seq[Ticker],LastBars : Seq[BarC], bars_properties : Seq[bars_property], barsNLast_Property : Seq[bar_last_deep]): Unit ={
    val listFut_Tickers   = for(thisTicker <- tickers  if bars_properties.map(bp => bp.ticker_id).contains(thisTicker.ticker_id)
                                                          /* thisTicker.ticker_id == 2*/
                                                          //OPTIMIZATION, don't do anything if there are no necessary ticks for bar calculation.
                                                          && (thisTicker.last_tick_ts_unx - {if (LastBars.nonEmpty && LastBars.map(b => b.ticker_id).contains(thisTicker.ticker_id))
                                                                                                LastBars.filter(b => b.ticker_id == thisTicker.ticker_id).map(lb => lb.ts_end_unx).max
                                                                                              else 0.toLong }
                                                             )/1000 >=
                                                              bars_properties.filter(bp => bp.ticker_id == thisTicker.ticker_id).map(bpt => bpt.bar_width_sec).min
                                                         ) yield /*Future*/{
                                                          logger.info("1. [run_background_calcs] inside for(thisTicker <- tickers) ticker_id="+thisTicker.ticker_id)
                                                          //here return Seq because can be more them one bar properties, different widths
                                                          val resThisTicker = calc_one_ticker(
                                                                                              thisTicker,
                                                                                              LastBars.filter(b => b.ticker_id == thisTicker.ticker_id),
                                                                                              bars_properties.filter(bp => bp.ticker_id == thisTicker.ticker_id),
                                                                                              barsNLast_Property.filter(bld => bld.ticker_id == thisTicker.ticker_id)
                                                                                             )
                                                          resThisTicker
                                                         }
  }


  def formatDate(dateValue : java.util.Date, dateFmt : String): String = {
    val sdf = new SimpleDateFormat(dateFmt)
    if (dateValue != null)
      sdf.format(dateValue)
    else
      null
  }



  /**
    * Main function for all calculation and operations.
    */
  def calc()  = {

    logger.debug("----------------------------------------------------------------------------------")
    val bars_properties : Seq[bars_property] = get_bars_property()
    for(oneProp <- bars_properties)
      logger.info("ticker_id = "+oneProp.ticker_id+" bar_width_sec = "+oneProp.bar_width_sec+" "+oneProp.is_enabled)

    logger.debug("----------------------------------------------------------------------------------")
    //Here we need read only !LAST! bars by each ticker_id and bar_width from property
    val seqBars : Seq[BarC] = get_Bars(bars_properties)//.flatten
    val lBars = new LastBars(seqBars)
    for(oneBar <- lBars.seqBars) logger.debug("LAST BAR: ticker_id = "+oneBar.ticker_id +" ddate="+formatDate(oneBar.ddate,"dd.MM.yyyy")+" width="+oneBar.bar_width_sec +" ts_end_unx = " +oneBar.ts_end_unx)

    logger.debug("----------------------------------------------------------------------------------")
    /**
      * For optimization purpose we get dataset tickers-ddates once and send it in get_Tickers.
      */
    //val ds_tickersddates : scala.List[Row] = get_ds_tickers_ddates()
    val tickers : Seq[Ticker] = get_Tickers(get_ds_tickers_ddates())

    logger.debug("----------------------------------------------------------------------------------")
    for (oneTicker <- tickers) {
      logger.debug(" ticker_id [ "+oneTicker.ticker_id+
              " ] "+oneTicker.ticker_code+" "+
              "  "+ formatDate(oneTicker.last_tick_ddate,"dd.MM.yyyy")+
              "   last_tick_ts = "+ formatDate(oneTicker.last_tick_ts,"dd.MM.yyyy HH:mm:ss")  +
              "   last_tick_ts_unx = "+oneTicker.last_tick_ts_unx)
    }

    logger.debug("----------------------------------------------------------------------------------")
    //run recaclulation each ticker by each bar property

    val barsNLast_Property : Seq[bar_last_deep] = get_bars_last_deeps()
    for (oneBarWdthLDeepProp <- barsNLast_Property) {
      logger.info("oneBarWdthLDeepProp : ticker_id="+oneBarWdthLDeepProp.ticker_id+" bar_width_sec="+oneBarWdthLDeepProp.bar_width_sec+" deep_count="+oneBarWdthLDeepProp.deep_count)
    }

    run_background_calcs(tickers, lBars.seqBars, bars_properties, barsNLast_Property)

  }





}


