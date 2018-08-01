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
class BarCalculator(session: Session) extends rowToX(session, LoggerFactory.getLogger(ReadCassandraExamples.getClass)) {

  val logger = LoggerFactory.getLogger(ReadCassandraExamples.getClass)

  /**
    * Read mts_meta.bars_property and return sequence of objects bars_property only with is_enabled=1
    */
  def get_bars_property() ={
    val results = session.execute("select * from mts_meta.bars_property;")
    val rsList = results.all()
    val bp = for(i <- 0 to rsList.size()-1) yield rowToBarProperty(rsList.get(i))//.asInstanceOf[bars_property]
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

    val seqOfSeqBarPropNN = seqOfSeqBarProp.filter(bp => bp.ddate!=null)
    for (oneBarProp <- seqOfSeqBarPropNN) yield {
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
  def calc_one_ticker(ticker : Ticker, bars : Seq[BarC], barsPropsTicker : Seq[bars_property]/*, barsNLast_Property : Seq[bar_last_deep]*/) : Seq[ticker_bars_save_result] ={
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
                                                                                                LastBars.filter(b => b.ticker_id == thisTicker.ticker_id).map(lb => lb.ts_end_unx).max
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
      logger.info("ticker_id = "+oneProp.ticker_id+" bar_width_sec = "+oneProp.bar_width_sec+" "+oneProp.is_enabled)

    logger.debug("----------------------------------------------------------------------------------")
    //Here we need read only !LAST! bars by each ticker_id and bar_width from property
    val seqBars : Seq[BarC] = get_Bars(bars_properties)
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

    run_background_calcs(tickers, lBars.seqBars, bars_properties)
  }


}


