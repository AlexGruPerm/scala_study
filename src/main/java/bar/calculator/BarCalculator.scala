package bar.calculator

import java.text.SimpleDateFormat
import java.util.Date

import com.datastax.driver.core.{LocalDate, Row, Session}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}



case class bars_property(ticker_id      :Int,
                         bar_width_sec  :Int,
                         is_enabled     :Int)

case class 	Bar(
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


case class LastBars(seqBars : Seq[Bar]) {
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
                                    ticker_id  :Int,
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
    new Bar(
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

        //val bars_this_ticket = bars.find(b => b.ticker_id == l_ticker_id)
        //println(">>> DEBUG: bars_this_ticket.size="+bars_this_ticket.size)
        /*
        val binfo = if (bars_this_ticket.nonEmpty) {
           (bars_this_ticket.map(b => b.ts_end).max,
            bars_this_ticket.map(b => b.ddate).max,
            bars_this_ticket.map(b => b.ts_end_unx).max)
        } else {
          (null,null,0.asInstanceOf[Long])
        }
        */

        val last_tick_ddate = s._2
        val last_tisk_tss : pair_ts_tsunx = getLastTickTs(l_ticker_id/*row.getInt("ticker_id")*/,s._1)

        new Ticker(
          ticker_id        = l_ticker_id,//row.getInt("ticker_id"),
          ticker_code      = row.getString("ticker_code"),
          /*
          last_bar_ddate   = binfo._2,
          last_bar_ts      = binfo._1,
          last_bar_ts_unx  = binfo._3,
          */
          last_tick_ddate  = last_tick_ddate,
          last_tick_ts     = last_tisk_tss.ts,
          last_tick_ts_unx = last_tisk_tss.ts_unx
        )

      }
      case None =>
        new Ticker(
        ticker_id        = 0,
        ticker_code      = " ",
          /*
        last_bar_ddate   = null,
        last_bar_ts      = null,
        last_bar_ts_unx  = 0,
          */
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

    for (bp <- bars_properties) yield {
      println("bp.ticker_id=" + bp.ticker_id + "  bp.bar_width_sec=" + bp.bar_width_sec)
      val resCommonBars = session.execute("""select distinct
                                            	      ticker_id,
                                            	      ddate,
                                            	      bar_width_sec
                                               from mts_bars.bars """)
      val rsCommonBars = resCommonBars.all()
      for (i <- 0 to rsCommonBars.size() - 1) yield {
        val rsComBTicker = rsCommonBars.get(i).getInt("ticker_id")
        val rsComBDate = rsCommonBars.get(i).getDate("ddate")
        val rsComBWidth = rsCommonBars.get(i).getInt("bar_width_sec")
        println("rsComBTicker=" + rsComBTicker + " rsComBDate=" + rsComBDate + " rsComBWidth=" + rsComBWidth)

        if (bp.bar_width_sec == rsComBWidth && bp.ticker_id == rsComBTicker) {
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

          val bound = resLastBarPrep.bind().setInt("tickerId", bp.ticker_id).setDate("maxDdate", rsComBDate).setInt("barWidth", bp.bar_width_sec)
          val rsBars = session.execute(bound).all()
          val seqBars = for(i <- 0 to rsBars.size()-1) yield rowToBar(rsBars.get(i))

        }

      }

    }
  }



/*
    val results = session.execute(""" select
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
                                            toUnixTimestamp(ts_end) as ts_end_unx
                                       from mts_bars.bars; """)
    val rsList = results.all()
    for(i <- 0 to rsList.size()-1) yield rowToBar(rsList.get(i))

  }
*/



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


  def calc_one_ticker(ticker : Ticker, bars : Seq[Bar]) : ticker_bars_save_result ={
    //check BARS property for this ticker
    println(" 2.[calc_one_ticker] for this ticker="+ticker.ticker_id+" exists next bar properties:")
     for (b <- bars) {
       println("  3. b.bar_width_sec="+b.bar_width_sec + " b.ts_end_unx = " + b.ts_end_unx+" diff_last_bar_unxts_last_tick = "+ (ticker.last_tick_ts_unx -b.ts_end_unx)/1000 +" seconds")
       //check new ticks data for bars calculation
       //if ()

     }


    new ticker_bars_save_result(ticker.ticker_id,1,2,100)
  }


  /**
    * Make main bar calculations with Futures.
    * @param tickers
    */
  def run_background_calcs(tickers : Seq[Ticker],bars : Seq[Bar]): Unit ={
    //Seq[Future[Ticker]]
    val listFut_Tickers : Seq[ticker_bars_save_result] = for(thisTicker <- tickers) yield {
                                                          println("1. [run_background_calcs] inside for(thisTicker <- tickers) ticker_id="+thisTicker.ticker_id)
                                                          val resThisTicker = calc_one_ticker(thisTicker,bars.filter(b => b.ticker_id==thisTicker.ticker_id))
                                                          resThisTicker
                                                          //new ticker_bars_save_result(thisTicker.ticker_id,1,2,100)
                                                         }
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

    //Here we need read only last bars by each ticker_id and bar_width from property

    val lBars = new LastBars(get_Bars(bars_properties))
    for(oneBar <- lBars.seqBars) println("oneBar.ticker_id = "+oneBar.ticker_id + " oneBar.ts_end_unx = " +oneBar.ts_end_unx)

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

    //run_background_calcs(tickers,bars)

  }





}


