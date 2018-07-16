package bar.calculator

import java.util.Date

import com.datastax.driver.core.{LocalDate, Row, Session}

import scala.concurrent.Future
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
                 ts_end_unx      :Int
                )

case class Ticker(
                   ticker_id        :Int,
                   ticker_code      :String,
                   last_bar_ddate   :java.util.Date,
                   last_bar_ts      :java.util.Date, // Max ts_end from bars by this ticker.
                   last_bar_ts_unx  :Int,
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
      row.getTimestamp("ddate"),
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
      row.getInt("ts_end_unx")
    )
  }

  /**
    * Return max(ddate) by ticker
    */
  def getLastTickDdate(p_ticker : Int) ={
    val prepared = session.prepare("select distinct ticker_id, ddate FROM mts_src.ticks where ticker_id = :tickerId;")
     // "select max(ddate) as ddate from mts_src.ticks where ticker_id=:tickerId;")
    val bound = prepared.bind().setInt("tickerId", p_ticker)
    val rsList = session.execute(bound).all()
    val res = for(i <- 0 to rsList.size()-1) yield //sList.get(i).getDate("ddate")
                                                   (
                                                     rsList.get(i).getDate("ddate"),
                                                     new Date(rsList.get(i).getDate("ddate").getMillisSinceEpoch)
                                                    )
    val getLastTickDdate_Res_Max = res.map(x => x._2).max
    val max_dates = res.find(x => x._2 == getLastTickDdate_Res_Max)
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
                                      order by ts desc
                                      LIMIT 1; """)

    //'2018-07-16'

    val bound = prepared.bind().setInt("tickerId", p_ticker).setDate("maxDDate",p_maxdate) //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    val rsList = session.execute(bound).all()
    val res = for(i <- 0 to rsList.size()-1) yield new pair_ts_tsunx(
                                                                    rsList.get(i).getTimestamp("ts"),
                                                                    rsList.get(i).getLong("tsunx")
                                                                    )
    res(0)
  }


  val rowToTicker = (row : Row, bars : Seq[Bar]) => {

    val bars_max_ts_end  : java.util.Date = if (bars.nonEmpty) bars.map(b => b.ts_end).max else null
    val bars_max_ddate   : java.util.Date = if (bars.nonEmpty) bars.map(b => b.ddate).max else null
    val bars_last_ts_unx : Int = if (bars.nonEmpty) bars.map(b=>b.ts_end_unx).max else 0

    val dd = getLastTickDdate(row.getInt("ticker_id"))

    dd match {
      case Some(s) => {
        val last_tick_ddate = s._2
        val last_tisk_tss : pair_ts_tsunx = getLastTickTs(row.getInt("ticker_id"),s._1)

        new Ticker(
          ticker_id        = row.getInt("ticker_id"),
          ticker_code      = row.getString("ticker_code"),
          last_bar_ddate   = bars_max_ddate,
          last_bar_ts      = bars_max_ts_end,
          last_bar_ts_unx  = bars_last_ts_unx,
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
  def get_Tickers(bars : Seq[Bar]) = {
    val results = session.execute("select * from mts_meta.tickers;")
    val rsList = results.all()
    for(i <- 0 to rsList.size()-1) yield rowToTicker(rsList.get(i),bars)

    /*
    def futGetTicker(r : Row, bars : Seq[Bar]) = Future {
      rowToTicker(r,bars)
    }

    val listFuturesFGetTicker = for(i <- 0 to rsList.size()-1) yield futGetTicker(rsList.get(i),bars)

    val res = for {
      c <- listFuturesFGetTicker
    } yield c

    res
    */

  }

  /**
    * Main function for all calculation and operations.
    */
  def calc()={
    val bars_properties : Seq[bars_property] = get_bars_property()
    for(oneProp <- bars_properties)
      println("ticker_id = "+oneProp.ticker_id+" bar_width_sec = "+oneProp.bar_width_sec+" "+oneProp.is_enabled)

    val bars : Seq[Bar] = get_Bars()
    for(oneBar <- bars) println("oneBar.ticker_id="+oneBar.ticker_id)

    println("-----------------------------------------")

    val tickers : IndexedSeq[Ticker] = get_Tickers(bars)

    for (oneTicker <- tickers) {
      println(" ticker_id  = "+oneTicker.ticker_id+
              " ticker_code  = "+oneTicker.ticker_code+
              " last_bar_ddate = "+oneTicker.last_bar_ddate+
              " last_bar_ts = "+oneTicker.last_bar_ts+
              " last_bar_ts_unx = "+oneTicker.last_bar_ts_unx+
              " last_tick_ddate = "+oneTicker.last_tick_ddate+
              " last_tick_ts = "+oneTicker.last_tick_ts+
              " last_tick_ts_unx = "+oneTicker.last_tick_ts_unx)}

  }

}


