package bar.calculator

import java.text.SimpleDateFormat
import java.util.Date

import com.datastax.driver.core.{LocalDate, Row, Session}

/*
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
*/

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

case class Ticker(
                   ticker_id        :Int,
                   ticker_code      :String,
                   last_bar_ddate   :java.util.Date,
                   last_bar_ts      :java.util.Date, // Max ts_end from bars by this ticker.
                   last_bar_ts_unx  :Long,
                   last_tick_ddate  :java.util.Date,
                   last_tick_ts     :java.util.Date,  // Max ts from mts_src.ticks for this ticker.
                   last_tick_ts_unx :Long//Int  // Max ts
                 )

case class pair_ts_tsunx(
                          ts      :java.util.Date,
                          ts_unx  :Long
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


  val rowToTicker = (row : Row, bars : Seq[Bar]) => {

     val dd = getLastTickDdate(row.getInt("ticker_id"))

    dd match {
      case Some(s) => {

        val l_ticker_id = row.getInt("ticker_id")
        val bars_this_ticket = bars.find(b => b.ticker_id == l_ticker_id)

        val binfo = if (bars_this_ticket.nonEmpty) {
           (bars_this_ticket.map(b => b.ts_end).max,
            bars_this_ticket.map(b => b.ddate).max,
            bars_this_ticket.map(b => b.ts_end_unx).max)
        } else {
          (null,null,0.asInstanceOf[Long])
        }

        val last_tick_ddate = s._2
        val last_tisk_tss : pair_ts_tsunx = getLastTickTs(l_ticker_id/*row.getInt("ticker_id")*/,s._1)

        new Ticker(
          ticker_id        = l_ticker_id,//row.getInt("ticker_id"),
          ticker_code      = row.getString("ticker_code"),
          last_bar_ddate   = binfo._2,
          last_bar_ts      = binfo._1,
          last_bar_ts_unx  = binfo._3,
          last_tick_ddate  = last_tick_ddate,
          last_tick_ts     = last_tisk_tss.ts,
          last_tick_ts_unx = last_tisk_tss.ts_unx
        )

      }
      case None =>
        new Ticker(
        ticker_id        = 0,
        ticker_code      = " ",
        last_bar_ddate   = null,
        last_bar_ts      = null,
        last_bar_ts_unx  = 0,
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

  def get_Bars() = {
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




  /**
    * Return seq of Ticker with additional information about timestamps.
    */
  def get_Tickers(bars : Seq[Bar], ds_tikers_ddates : java.util.List[Row]) = {
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
        rowToTicker(ds_tikers_ddates.get(i),bars)
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



  def run_background_calcs(tickers : Seq[Ticker]): Unit ={


    run_background_calcs(tickers)
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

    val bars : Seq[Bar] = get_Bars()
    for(oneBar <- bars) println("oneBar.ticker_id = "+oneBar.ticker_id + " oneBar.ts_end_unx = " +oneBar.ts_end_unx)

    println("----------------------------------------------------------------------------------")

    /**
      * For optimization purpose we get dataset tickers-ddates once and send it in get_Tickers.
      */
    //val ds_tickersddates : scala.List[Row] = get_ds_tickers_ddates()
    val tickers : Seq[Ticker] = get_Tickers(bars, get_ds_tickers_ddates())

    println("----------------------------------------------------------------------------------")
    for (oneTicker <- tickers) {

      println(" ticker_id  = "+oneTicker.ticker_id+
              " ticker_code  = "+oneTicker.ticker_code+
              " last_bar_ddate = "+ formatDate(oneTicker.last_bar_ddate,"dd.MM.yyyy") +
              " last_bar_ts = " + formatDate(oneTicker.last_bar_ts,"dd.MM.yyyy HH:mm:ss") +
              " last_bar_ts_unx = "+oneTicker.last_bar_ts_unx+
              " last_tick_ddate = "+ formatDate(oneTicker.last_tick_ddate,"dd.MM.yyyy")+
              " last_tick_ts = "+ formatDate(oneTicker.last_tick_ts,"dd.MM.yyyy  HH:mm:ss")  +
              " last_tick_ts_unx = "+oneTicker.last_tick_ts_unx)
    }
    //run_background_calcs(tickers)

  }





}


