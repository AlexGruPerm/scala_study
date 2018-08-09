package bar.calculator

import bar.{ReadCassandraExamples, rowToX}
import java.text.SimpleDateFormat
import java.util.Date

import com.datastax.driver.core
import com.datastax.driver.core.{LocalDate, Row, Session}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters

case class bars_property(ticker_id      :Int,
                         bar_width_sec  :Int,
                         is_enabled     :Int)


case class LastBars(seqBars : Seq[BarC]) {
  def size : Int = seqBars.size
  val logger = LoggerFactory.getLogger(ReadCassandraExamples.getClass)

  logger.debug("Constructor of LastBars size="+size)

}

case class Ticker(
                   ticker_id        :Int,
                   ticker_code      :String,
                   last_tick_ddate  :java.util.Date,
                   last_tick_ts     :Long/*java.util.Date*/,  // Max ts from mts_src.ticks for this ticker.
                   last_tick_ts_unx :Long             // Max ts
                 )

case class pair_ts_tsunx(
                          ts      :Long,//java.util.Date,
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
class BarCalculator(session: Session) extends rowToX(session, LoggerFactory.getLogger(ReadCassandraExamples.getClass)) {

  val logger = LoggerFactory.getLogger(ReadCassandraExamples.getClass)

  /**
    * Read mts_meta.bars_property and return sequence of objects bars_property only with is_enabled=1
    */
  def get_bars_property() ={
    val results = session.execute("select * from mts_meta.bars_property;")
    val rsList = results.all()
    val bp = for(i <- 0 to rsList.size()-1) yield rowToBarProperty(rsList.get(i))
    bp.filter(b => b.is_enabled==1)
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

    logger.info("seqOfSeqBarProp.size="+seqOfSeqBarProp.size)
    val seqOfSeqBarPropNN = seqOfSeqBarProp.filter(bp => bp.ddate != null)
    logger.info("seqOfSeqBarPropNN.size="+seqOfSeqBarPropNN.size)

    for (oneBarProp <- seqOfSeqBarPropNN) yield {
      logger.info("SEARCH BUG: ticker_id ["+oneBarProp.ticker_id+"]  MAX(ddate)="+oneBarProp.ddate+"  getDaysSinceEpoch="+oneBarProp.ddate.getDaysSinceEpoch)
      val bound = resLastBarPrep.bind().setInt("tickerId", oneBarProp.ticker_id)
                                       .setDate("maxDdate", oneBarProp.ddate)
                                       .setInt("barWidth", oneBarProp.bar_width_sec)
      val rsBar = session.execute(bound).one()
      rowToBar(rsBar)
    }

  }

  /**
    * Return seq of Ticker with additional information about timestamps.
    */
  def get_Tickers(ds_tikers_ddates : java.util.List[Row]) = {
    val listTicker : Seq[Ticker] = for(i <- 0 to ds_tikers_ddates.size-1) yield
        rowToTicker(ds_tikers_ddates.get(i))
    //logger.info("get_Tickers listTicker.size="+listTicker.size)
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


  /**
    *
    * @return Seq of ticks, only ts,ask,bid.  ts - trade server time.
    */
  def read_Ticks_by_ts_interval(p_ticker_id : Int, p_ts_begin :Long, p_ts_end : Long) : Seq[TinyTick] = {
    val bound = resTinyTicksByTsInterval.bind().setInt("tickerId",  p_ticker_id)
                                               .setLong("ts_begin", p_ts_begin)
                                               .setLong("ts_end",   p_ts_end)
    val rsTicks : Seq[TinyTick] = JavaConverters.asScalaIteratorConverter(session.execute(bound).all().iterator())
                                                .asScala
                                                .toSeq.map(tick => new TinyTick(tick.getLong("ts"),
                                                                                tick.getDouble("ask"),
                                                                                tick.getDouble("bid")))
                                                       .sortBy(ft => ft.ts) // sort by asc, head the oldest element.
    if (rsTicks.nonEmpty) {
      logger.info("    READ " + rsTicks.size + " TICKS.")
      if (rsTicks.size >= 2)
        logger.info("     FIRST TICK TS:"+rsTicks.head.ts+" LAST TICK TS:"+rsTicks.last.ts)
      else
        logger.info("    rsTicks.size less then 2.")
    } else {
      logger.info("    READ 0 TICKS.")
    }
    rsTicks
  }


  /** ===============================================================================================================
    *  bars must contain only one Last bars(no one) by this ticker (for each widths) if this exists in db.
    * @param ticker
    * @param bars - Last bars by each width.
    * @return
    */
  def calc_one_ticker(ticker : Ticker, bars : Seq[BarC], barsPropsTicker : Seq[bars_property]) : Seq[ticker_bars_save_result] ={
    logger.info("  2.[calc_one_ticker] ticker=" + ticker.ticker_id)

    /**
      * OPTIMIZATION, here we need extract min ddate also from bars.
      * And compare with ddate from ticker, if they are equal then use another query to get ticks by interval:
      * with sending ddate to reduce partitions scan.
      */
    val seqMinsBarTsEnd = for (bp <- barsPropsTicker) yield
                               bars.filter(b => b.bar_width_sec == bp.bar_width_sec).map(b => b.ts_end).reduceOption(_ min _).getOrElse(0.toLong)

    logger.info("   2.1 [calc_one_ticker] seqMinsBarTsEnd.size="+seqMinsBarTsEnd.size)

    val min_ts_in_bars = {if (barsPropsTicker.size==0)
                               0.toLong
                             else
                             seqMinsBarTsEnd.min}

    logger.info("   2.2 [calc_one_ticker] min_unxts_in_bars="+min_ts_in_bars)
    logger.info("    BEFORE session.prepare resTicksByTsInterval min_unxts_in_bars=" + min_ts_in_bars + " ticker.last_tick_ts="+ticker.last_tick_ts+" ticker.last_tick_ts_unx="+ticker.last_tick_ts)
    logger.info(" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    logger.info(" NO 0 each time,  min_ts_in_bars = "+min_ts_in_bars+"   bars.size="+bars.size)
    logger.info(" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")

    val rsTicks : Seq[TinyTick] = read_Ticks_by_ts_interval(ticker.ticker_id,
                                                            min_ts_in_bars,
                                                            ticker.last_tick_ts);

    val res = for (bp <- barsPropsTicker if rsTicks.nonEmpty) yield {

       logger.debug("    3. bar_width_sec = " + bp.bar_width_sec)
       val lastBar_ByWidth = bars.filter(b => b.bar_width_sec == bp.bar_width_sec)
       val lastBar_ts_end : Long = if (lastBar_ByWidth.isEmpty) 0
                                        else lastBar_ByWidth.head.ts_end

       //val lastBat_tsendunx_Diff_ticker_lstunx_Sec = (ticker.last_tick_ts - lastBar_ts_end )/1000
       val dataWidthSec = ticker.last_tick_ts - Math.max(lastBar_ts_end,rsTicks.head.ts) // removed because short ts format /1000

       logger.debug("    !!!   4.   ticker.last_tick_ts="+ticker.last_tick_ts+" lastBar_ts_end="+lastBar_ts_end+" rsTicks.head.db_tsunx="+rsTicks.head.ts+"   dataWidthSec=" + dataWidthSec)


      //Value lastBat_tsendunx_Diff_ticker_lstunx_Sec can be extremaly BIG because lastBar_ts_end can be equal 0.
       if (dataWidthSec/*lastBat_tsendunx_Diff_ticker_lstunx_Sec*/ > bp.bar_width_sec) {
         logger.debug("        5. diff more then width. Start calculation for ticker, width, ts in ticks from "+ min_ts_in_bars +" to "+ticker.last_tick_ts+" min tsunx_from_bars="+min_ts_in_bars)

         //OLD BARS BY TICK, need seconds.
         // val seqSeqTicks : Seq[Seq[FinTick]] = rsTicks.sliding(bp.bar_width_sec,bp.bar_width_sec).filter(x => (x.size==bp.bar_width_sec)).toSeq
         //val fIdx = rsTicks.head.ts.getTime
         //val lIdx = rsTicks.last.ts.getTime
         val barsSides = rsTicks.head.ts.to(rsTicks.last.ts).by(bp.bar_width_sec/* *1000 */) //removed *1000 because short ts format

         logger.info("barsSides.size="+barsSides.size)

         logger.info("from : "+rsTicks.head.ts)
         logger.info("to   : "+rsTicks.last.ts)

         val seqBarSides = for ((bs,idx) <- barsSides.zipWithIndex) yield {
            //logger.info("bs="+bs+"  idx.toInt="+idx.toInt)
           (bs,idx.toInt)
         }


         logger.info("seqBarSides.size= "+seqBarSides.size)

         val seqBar2Sides = for(i <- 0 to seqBarSides.size-1) yield {
           if (i < seqBarSides.last._2)
             (seqBarSides(i)._1, seqBarSides(i+1)._1, seqBarSides(i)._2+1)
           else
             (seqBarSides(i)._1, seqBarSides(i)._1 /*+ bp.bar_width_sec*1000*/, seqBarSides(i)._2+1)  //#########
         }

         /*
         for (bts <- seqBar2Sides) {
           logger.info("brs ("+ bts._1 + " - " +bts._2+")")
         }
         */

         def getGroupThisElement(elm : Long)={
           //changed  (bs._2 - bs._1)/1000.toInt because short ts format.
           seqBar2Sides.find(bs => ((bs._1 <= elm && bs._2 > elm) && (bs._2 - bs._1) == bp.bar_width_sec)).map(x => x._3).getOrElse(0)
         }

         val seqSeqTicks = rsTicks.groupBy(elm => getGroupThisElement(elm.ts)).filter(seqT => seqT._1!=0 ).toSeq.sortBy(gr => gr._1)
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

           //           logger.info("EACH BAR: p_ddate="+core.LocalDate.fromMillisSinceEpoch( b.ddate.getTime())+"   b.ddate="+b.ddate+"   p_ts_begin="+b.ts_begin)
           //logger.info("EACH BAR: p_ddate="+ b.ddate+"   b.ddate="+b.ddate+"   p_ts_begin="+b.ts_begin)

           val boundSaveBar = prepSaveBar.bind()
             .setInt("p_ticker_id", ticker.ticker_id)
             .setDate("p_ddate",  core.LocalDate.fromMillisSinceEpoch(b.ddate*1000))
             //.setLong("p_ddate",  b.ddate)
             .setInt("p_bar_width_sec",b.bar_width_sec)
             .setLong("p_ts_begin", b.ts_begin)
             .setLong("p_ts_end", b.ts_end)
             .setDouble("p_o",b.o)
             .setDouble("p_h",b.h)
             .setDouble("p_l",b.l)
             .setDouble("p_c",b.c)
             .setDouble("p_h_body",b.h_body)
             .setDouble("p_h_shad",b.h_shad)
             .setString("p_btype",b.btype)
             .setInt("p_ticks_cnt",b.ticks_cnt)
             .setDouble("p_disp",b.disp)
             .setDouble("p_log_co",b.log_co)
           session.execute(boundSaveBar)
         }

         //UPSERT LAST BAR FROM CALCED BARS seqBarsCalced
           val lastBarFromBars = seqBarsCalced.filter(b => b.ts_end == seqBarsCalced.map(bfs => bfs.ts_end).max).head //only one !?

           logger.info("LAST BAR: lastBarFromBars.ddate="+lastBarFromBars.ddate)

           val boundSaveBarLast = prepSaveOnlineLastBars.bind()
             .setInt("p_ticker_id", ticker.ticker_id)
             .setInt("p_bar_width_sec", lastBarFromBars.bar_width_sec)
             .setLong("p_ts_begin", lastBarFromBars.ts_begin)
             .setLong("p_ts_end", lastBarFromBars.ts_end)
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



         }

         logger.info("                ")
         logger.debug("               ")

          new ticker_bars_save_result(
            ticker_id            = ticker.ticker_id,
            bar_width_sec        = bp.bar_width_sec,
            prev_last_bar_ts_unx = lastBar_ts_end,
            curr_last_bar_ts_unx = if (seqBarsCalced.nonEmpty) seqBarsCalced.map(b => b.ts_end).max else 0,
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

    logger.debug(" ----------------------------------------------------------------------")
    res
  }




  /** ===============================================================================================================
    * Make main bar calculations with Futures.
    * @param tickers
    */
  def run_background_calcs(tickers : Seq[Ticker],LastBars : Seq[BarC], bars_properties : Seq[bars_property]): Unit ={
    val listFut_Tickers   = for(thisTicker <- tickers  if bars_properties.map(bp => bp.ticker_id).contains(thisTicker.ticker_id)
                                                          /* thisTicker.ticker_id == 2*/
                                                          //OPTIMIZATION, don't do anything if there are no necessary ticks for bar calculation.
                                                          && (thisTicker.last_tick_ts_unx - {if (LastBars.nonEmpty && LastBars.map(b => b.ticker_id).contains(thisTicker.ticker_id))
                                                                                                LastBars.filter(b => b.ticker_id == thisTicker.ticker_id).map(lb => lb.ts_end).max
                                                                                              else 0.toLong }
                                                             )/1000 >= bars_properties.filter(bp => bp.ticker_id == thisTicker.ticker_id).map(bpt => bpt.bar_width_sec).min
                                                         ) yield /*Future*/{
                                                          logger.info("1. [run_background_calcs] inside for(thisTicker <- tickers) ticker_id="+thisTicker.ticker_id)
                                                          //here return Seq because can be more them one bar properties, different widths
                                                          val resThisTicker = calc_one_ticker(
                                                                                              thisTicker,
                                                                                              LastBars.filter(b => b.ticker_id == thisTicker.ticker_id),
                                                                                              bars_properties.filter(bp => bp.ticker_id == thisTicker.ticker_id)
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
      logger.info("BAR PROPERTY:    ticker_id = "+oneProp.ticker_id+" bar_width_sec = "+oneProp.bar_width_sec)

    logger.debug("----------------------------------------------------------------------------------")
    //Here we need read only !LAST! bars by each ticker_id and bar_width from property
    val seqBars : Seq[BarC] = get_Bars(bars_properties)
    val lBars = new LastBars(seqBars)
    for(oneBar <- lBars.seqBars) logger.debug("LAST BAR: ticker_id = "+oneBar.ticker_id +" ddate="+formatDate(oneBar.ddate,"dd.MM.yyyy")+" width="+oneBar.bar_width_sec +" ts_end_unx = " +oneBar.ts_end)

    logger.debug("----------------------------------------------------------------------------------")
    /**
      * For optimization purpose we get dataset tickers-ddates once and send it in get_Tickers.
      */
    //val ds_tickersddates : scala.List[Row] = get_ds_tickers_ddates()
    val tickers : Seq[Ticker] = get_Tickers(get_ds_tickers_ddates())

    logger.debug("----------------------------------------------------------------------------------")
    for (oneTicker <- tickers if oneTicker.ticker_id != 0) {
      logger.info(" ticker_id [ "+oneTicker.ticker_id+
              " ] "+oneTicker.ticker_code+" "+
              "  "+ formatDate(oneTicker.last_tick_ddate,"dd.MM.yyyy")+
              "   last_tick_ts(Long) = "+ oneTicker.last_tick_ts  +
              "   last_tick_ts_unx = "+oneTicker.last_tick_ts_unx)
    }
    logger.debug("----------------------------------------------------------------------------------")

    run_background_calcs(tickers, lBars.seqBars, bars_properties)
  }


}


