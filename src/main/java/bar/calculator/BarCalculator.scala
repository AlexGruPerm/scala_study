package bar.calculator

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Date

import com.datastax.driver.core.{LocalDate, Row, Session}
import io.netty.util.concurrent.Promise

import scala.collection.JavaConverters
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}



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

  println("Constructor of LastBars size="+size)

  //if(i < 0)
  //  throw new IllegalArgumentException("the number must be non-negative.")



}


case class Ticker(
                   ticker_id        :Int,
                   ticker_code      :String,
                   // ??? bars has diff width -------------------------
                   /*
                   last_bar_ddate   :java.util.Date,
                   last_bar_ts      :java.util.Date, // Max ts_end from bars by this ticker.
                   last_bar_ts_unx  :Long,
                   */
                   //-------------------------
                   last_tick_ddate  :java.util.Date,
                   last_tick_ts     :java.util.Date,  // Max ts from mts_src.ticks for this ticker.
                   last_tick_ts_unx :Long//Int  // Max ts
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

  val rowToBarProperty = (row: Row) => {
    //if (row.getInt("is_enabled") == 1)
      new bars_property(row.getInt("ticker_id"), row.getInt("bar_width_sec"), row.getInt("is_enabled"))
  }

  val rowToBar = (row : Row) => {
    new BarC(
      row.getInt("ticker_id"),
      //row.getTimestamp("ddate"),
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
    val getLastTickDdate_Res_Max = res.map(x => x._3).max
    val max_dates = res.find(x => x._3 == getLastTickDdate_Res_Max &&  x._1 == p_ticker).map(x => (x._2,x._3))
    println("[getLastTickDdate] p_ticker="+p_ticker+"  max_dates=["+max_dates+"]")
    max_dates

  }


  /**
    * Return max(ts),max(ts) as UNixTimeStamp by ticker
    */
  def getLastTickTs(p_ticker : Int, p_maxdate : LocalDate) = {
    val prepared = session.prepare(""" select max(ts) as ts,
                                              toUnixTimestamp(max(ts)) as tsunx
                                       from mts_src.ticks
                                      where ticker_id = :tickerId and
                                            ddate     = :maxDDate
                                   """)
    /*
    tail of old query - order by ts desc LIMIT 1;
    */

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



  def get_Bars(bars_properties : Seq[bars_property]) = {
    //preparation once, outside next for loop
    val resLastDateBar = session.prepare("""select max(ddate) as ddate
                                              from mts_bars.bars
                                             where ticker_id     = :tickerId and
                                                   bar_width_sec = :barWidth
                                            ALLOW FILTERING; """);

    val seqOfSeqBarProp: Seq[barProps] =
      for (bp <- bars_properties) yield {
        val boundLastDate = resLastDateBar.bind().setInt("tickerId", bp.ticker_id)
                                                 .setInt("barWidth", bp.bar_width_sec)
        val rsLastDate = session.execute(boundLastDate).one()
          new barProps(bp.ticker_id, rsLastDate.getDate("ddate"), bp.bar_width_sec)
    }

    val seqOfSeqBarPropNN = seqOfSeqBarProp.filter(bp => bp.ddate!=null)

    //preparation once, outside next for loop
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

    for (oneBarProp <- seqOfSeqBarPropNN) yield {
      //println("inside for (oneBarProp <- seqOfSeqBarProp) ticker_id=" + oneBarProp.ticker_id + " ddate=" + oneBarProp.ddate + " bar_width_sec=" + oneBarProp.bar_width_sec)
      val bound = resLastBarPrep.bind().setInt("tickerId", oneBarProp.ticker_id)
                                       .setDate("maxDdate", oneBarProp.ddate)
                                       .setInt("barWidth", oneBarProp.bar_width_sec)
      val rsBar = session.execute(bound).one()
      //println("___ inside for (oneBarProp <- seqOfSeqBarProp) ticker_id="+rsBar.getInt("ticker_id")+" ts_end_unx="+rsBar.getLong("ts_end_unx"))
      rowToBar(rsBar)
    }

  }




  /**
    * Return seq of Ticker with additional information about timestamps.
    */
  def get_Tickers(ds_tikers_ddates : java.util.List[Row]) = {
    /*
    Examples of Futures!
    val results = session.execute("select * from mts_meta.tickers;")
    val rsList = results.all()

    val listFuturesFGetTicker : Seq[Future[Ticker]] = for(i <- 0 to rsList.size()-1) yield
       Future {
               rowToTicker(rsList.get(i),bars)
              }

    val allFutiresGetTicker: Future[Seq[Ticker]] = Future.sequence(listFuturesFGetTicker)
    val tickersRes: Seq[Ticker] = Await.result(allFutiresGetTicker, 30.seconds)

    tickersRes
    */
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
    *
    * @param ticker
    * @param bars - Last bars by each width.
    * @return
    */
  def calc_one_ticker(ticker : Ticker, bars : Seq[BarC], barsPropsTicker : Seq[bars_property]) : Seq[ticker_bars_save_result] ={
    println("  2.[calc_one_ticker] ticker=" + ticker.ticker_id)

    val seqMinsBarUnxtsEnd = for (bp <- barsPropsTicker) yield
      bars.filter(b => b.bar_width_sec == bp.bar_width_sec).map(b => b.ts_end_unx).reduceOption(_ min _).getOrElse(0.toLong)

    val min_unxts_in_bars = seqMinsBarUnxtsEnd.min

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

    val bound = resTicksByTsInterval.bind().setInt("tickerId", ticker.ticker_id)
                                           .setTimestamp("ts_begin",  new Date(min_unxts_in_bars))
                                           .setTimestamp("ts_end",  new Date(ticker.last_tick_ts_unx))

    //val rsTicksRows = session.execute(bound).all()
    //one ticks reading for all widths.
    val rsTicks : Seq[FinTick] = JavaConverters.asScalaIteratorConverter(session.execute(bound).all().iterator())
                                               .asScala
                                               .toSeq.map(tick => new FinTick(tick.getTimestamp("ts"),
                                                                              tick.getDouble("ask"),
                                                                              tick.getDouble("bid")))
                                               .sortBy(ft => ft.ts)

    println("      tick 1: "+rsTicks.head.ts.getTime)
    println("      tick 2: "+rsTicks.tail.head.ts.getTime)
    println("      tick 3: "+rsTicks.tail.tail.head.ts.getTime)
    println(" >>>>>>>>>>>>> READED FROM DB "+ rsTicks.size+" TICKS.")

    val res = for (bp <- barsPropsTicker) yield {

       println("    3. bar_width_sec = " + bp.bar_width_sec)
       val lastBar_ByWidth = bars.filter(b => b.bar_width_sec == bp.bar_width_sec)
       val lastBar_ts_end_unx : Long = if (lastBar_ByWidth.isEmpty) 0
                                        else lastBar_ByWidth.head.ts_end_unx

       val lastBat_tsendunx_Diff_ticker_lstunx_Sec = (ticker.last_tick_ts_unx -lastBar_ts_end_unx)/1000

       println("      4.  ts_end_unx = " + lastBar_ts_end_unx + " Diff[seconds] = " + lastBat_tsendunx_Diff_ticker_lstunx_Sec)
       if (lastBat_tsendunx_Diff_ticker_lstunx_Sec > bp.bar_width_sec) {
         println("        5. diff more then width. Start calculation for ticker, width, ts in ticks from "+ min_unxts_in_bars +" to "+ticker.last_tick_ts_unx+" min tsunx_from_bars="+min_unxts_in_bars)

         val seqSeqTicks : Seq[Seq[FinTick]] = rsTicks.sliding(bp.bar_width_sec,bp.bar_width_sec).filter(x => (x.size==bp.bar_width_sec)).toSeq

         val seqBarsCalced = for (seqTicksOneBar <- seqSeqTicks) yield
                           new Bar(
                                    p_ticker_id =ticker.ticker_id,
                                    p_bar_width_sec=bp.bar_width_sec,
                                    barTicks = seqTicksOneBar
                                   )


         println(" >>>>>>>>>>>>> CALCULATED BARS "+ seqBarsCalced.size)

         println("                ")
         println("                ")

         //SAVE BARS

          new ticker_bars_save_result(
            ticker_id            = ticker.ticker_id,
            bar_width_sec        = bp.bar_width_sec,
            prev_last_bar_ts_unx = lastBar_ts_end_unx,
            curr_last_bar_ts_unx = seqBarsCalced.map(b => b.ts_end_unx).max,
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

    println("   3. b.bar_width_sec="+thisTicketLastBar.bar_width_sec +
            " b.ts_end_unx = " + thisTicketLastBar.ts_end_unx+
            " diff_last_bar_unxts_last_tick = "+ (ticker.last_tick_ts_unx - thisTicketLastBar.ts_end_unx)/1000 +
           " seconds")

    if ((ticker.last_tick_ts_unx - thisTicketLastBar.ts_end_unx)/1000 > thisTicketLastBar.bar_width_sec) {
      println("         --- "+(ticker.last_tick_ts_unx -thisTicketLastBar.ts_end_unx)/1000 +">"+ thisTicketLastBar.bar_width_sec+" CALCULATE THIS WIDTH_TICKER PAIR")
      //check new ticks data for bars calculation
    }
*/

    // так мы пропускаем тикеты по которым вообще нет баров из расчета баров для них !!!
    //
    //  Наоборот поменять чтобы старт был с тикара! и уже дальше смотри есть по нему какие-то посчитанные бары или нет!
    //
    println(" ----------------------------------------------------------------------")
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
  def run_background_calcs(tickers : Seq[Ticker],bars : Seq[BarC], bars_properties : Seq[bars_property]): Unit ={
    //Seq[Future[Ticker]]
    //scala.collection.immutable.Vector of Futures
    val listFut_Tickers  /*Seq[Seq[ticker_bars_save_result]]*/ = for(thisTicker <- tickers /*if thisTicker.ticker_id == 2*/ ) yield Future{
                                                          println("1. [run_background_calcs] inside for(thisTicker <- tickers) ticker_id="+thisTicker.ticker_id)
                                                          //here return Seq because can be more them one bar properties, different widths
                                                          val resThisTicker = calc_one_ticker(
                                                                                              thisTicker,
                                                                                              bars.filter(b => b.ticker_id == thisTicker.ticker_id),
                                                                                              bars_properties.filter(bp => bp.ticker_id == thisTicker.ticker_id)
                                                                                             )
                                                          resThisTicker
                                                         }
    println("listFut_Tickers class="+listFut_Tickers.getClass.getName)

    val futureOfList = Future.sequence(listFut_Tickers)

    futureOfList onComplete {
      case Success(x)  => println("Success!!! " + x)
      case Failure(ex) => println("Failed !!! " + ex)
    }

    /*
        val resBarsCals = for {
          c <- listFut_Tickers
        } yield c()


         {
          case Success(x) => println(s"OK result = $x")
          case Failure(e) => e.printStackTrace
        }
        */


    /*
      Future {
        calcThisTickersAll
      }
    */


    //если все футуры выполнились, то делаем паузу и запускам опять всё ??!!
    //ONE CALL WHILE !!!   run_background_calcs(tickers)
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
  def calc()={
    println("----------------------------------------------------------------------------------")
    val bars_properties : Seq[bars_property] = get_bars_property()
    for(oneProp <- bars_properties)
      println("ticker_id = "+oneProp.ticker_id+" bar_width_sec = "+oneProp.bar_width_sec+" "+oneProp.is_enabled)

    println("----------------------------------------------------------------------------------")

    //Here we need read only !LAST! bars by each ticker_id and bar_width from property
    val seqBars : Seq[BarC] = get_Bars(bars_properties)//.flatten
    val lBars = new LastBars(seqBars)
    for(oneBar <- lBars.seqBars) println("LAST BAR: ticker_id = "+oneBar.ticker_id +" ddate="+formatDate(oneBar.ddate,"dd.MM.yyyy")+" width="+oneBar.bar_width_sec +" ts_end_unx = " +oneBar.ts_end_unx)

    println("----------------------------------------------------------------------------------")

    /**
      * For optimization purpose we get dataset tickers-ddates once and send it in get_Tickers.
      */
    //val ds_tickersddates : scala.List[Row] = get_ds_tickers_ddates()
    val tickers : Seq[Ticker] = get_Tickers(get_ds_tickers_ddates())

    println("----------------------------------------------------------------------------------")
    for (oneTicker <- tickers) {

      println(" ticker_id [ "+oneTicker.ticker_id+
              " ] "+oneTicker.ticker_code+" "+
              "  "+ formatDate(oneTicker.last_tick_ddate,"dd.MM.yyyy")+
              "   last_tick_ts = "+ formatDate(oneTicker.last_tick_ts,"dd.MM.yyyy HH:mm:ss")  +
              "   last_tick_ts_unx = "+oneTicker.last_tick_ts_unx)
    }

    println("----------------------------------------------------------------------------------")
    //run recaclulation each ticker by each bar property

    run_background_calcs(tickers, lBars.seqBars, bars_properties)

  }





}


