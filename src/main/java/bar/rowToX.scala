package bar

import java.util.Date

import bar.calculator._
import com.datastax.driver.core.{LocalDate, Row, Session}
import org.slf4j.Logger

import scala.collection.JavaConverters

abstract class rowToX(val session: Session,val alogger: Logger) {



  val resListAdvisers = session.prepare(""" select adviser_id,ticker_id,func_name
                                              from mts_meta.trade_advisers_ticker
                                             where is_enabled = 1 allow filtering; """)


  val resLastDateBar = session.prepare("""select max(ddate) as ddate
                                              from mts_bars.bars
                                             where ticker_id     = :tickerId and
                                                   bar_width_sec = :barWidth
                                            ALLOW FILTERING; """);

  /*
   max(ts) as ts,
   toUnixTimestamp(max(ts)) as tsunx
  */
  val prepared = session.prepare(""" select max(ts)       as ts,
                                            max(db_tsunx) as tsunx
                                       from mts_src.ticks
                                      where ticker_id = :tickerId and
                                            ddate     = :maxDDate
                                   """)

  val prepListTickers = session.prepare(""" select * from mts_meta.tickers """)


  val resNLastBars = session.prepare(
    """ select
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
             disp,
             log_co
        from mts_bars.bars
        where
             ticker_id     = :tickerId and
             ddate         = :pddate   and
             bar_width_sec = :p_bar_width_sec
        order by ts_end desc
        LIMIT :plimit allow filtering;
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
                  ticks_cnt,
                  disp,
                  log_co
               from mts_bars.bars
               where ticker_id     = :tickerId and
                     ddate         = :maxDdate and
                     bar_width_sec = :barWidth;
            """)

  val resTicksByTsInterval = session.prepare(
    """
          select ticker_id,
                 ddate,
                 ts,
                 db_tsunx,
                 bid,
                 ask
            from mts_src.ticks
           where ticker_id = :tickerId and
                 ts > :ts_begin and
                 ts < :ts_end
                  ALLOW FILTERING;
    """)

  val resTinyTicksByTsInterval = session.prepare(
    """
          select ts,
                 bid,
                 ask
            from mts_src.ticks
           where ticker_id = :tickerId and
                 ts >= :ts_begin and
                 ts <= :ts_end
                  ALLOW FILTERING;
    """)

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
            disp,
            log_co
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
            :p_disp,
            :p_log_co
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

  val prepSaveAdviserRes = session.prepare(""" insert into mts_meta.trade_advisers_results(adviser_id,ts,main_way) values(:p_adviser_id,:p_ts,:p_main_way); """)

  val rowToBarProperty = (row: Row) => {
    //if (row.getInt("is_enabled") == 1)
    new bars_property(row.getInt("ticker_id"), row.getInt("bar_width_sec"), row.getInt("is_enabled"))
  }

  val prepGetLastNBars = session.prepare(
    """  select
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
                toUnixTimestamp(ts_end) as ts_end_unx,
                ticks_cnt,
                disp,
                log_co
           from mts_bars.bars
          where ticker_id     = :p_ticker_id and
                bar_width_sec = :p_bar_width_sec
          limit :p_limit
          allow filtering
    """)


  val queryTickersWW =
    """ select ticker_id
          from mts_meta.bars_property
         where bar_width_sec = 600 and
               is_enabled    = 1
         allow filtering; """

  val prepqueryTickersWW = session.prepare(queryTickersWW)

  val queryMinMax = """select count(ticker_id)  as bars_cnt,
                               min(ts_begin)    as ts_begin,
                               max(ts_end)      as ts_end
                        from mts_bars.bars
                       where ticker_id     = :p_ticker_id and
                             bar_width_sec = :p_width_sec
                       allow filtering """

  val prepQuery = session.prepare(queryMinMax)

  val queryBars = """      select
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
                                  disp,
                                  log_co
                             from mts_bars.bars
                             where
                                  ticker_id     = :p_ticker_id and
                                  bar_width_sec = :p_width_sec and
                                  ts_begin     >= :p_ts_begin and
                                  ts_end       <= :p_ts_end
                             allow filtering;  """

  val prepQueryBars = session.prepare(queryBars)


  val queryInsertRes =
    """insert into mts_meta.way_adviser_n_hours(
      	ticker_id,
      	bar_width_sec,
      	ts_res,
          way,
          deep_sec,
          adv_bars_in_part,
          p1_size_bars,
          p2_size_bars,
          p3_size_bars,
          p1_cnt,
          p2_cnt,
          p3_cnt,
          p1_logco,
          p2_logco,
          p3_logco)
         values(
      	  :ticker_id,
      	  :bar_width_sec,
      	  :ts_res,
          :way,
          :deep_sec,
          :adv_bars_in_part,
          :p1_size_bars,
          :p2_size_bars,
          :p3_size_bars,
          :p1_cnt,
          :p2_cnt,
          :p3_cnt,
          :p1_logco,
          :p2_logco,
          :p3_logco
         );
    """
  val prepInsertRes = session.prepare(queryInsertRes)



  val prepTickersWidths = session.prepare(""" select ticker_id,bar_width_sec from mts_meta.bars_property """)

  //val prepTickersWidths = session.prepare("""select ticker_id,bar_width_sec from mts_meta.bars_property where ticker_id=1 and bar_width_sec=30;""")


  val prepMaxTsEndFuture = session.prepare(""" select max(ts_end) as max_ts_end
                                                 from mts_bars.bars_future
                                                where ticker_id     = :p_ticker_id and
                                                      bar_width_sec = :p_bar_width_sec """)


  val prepReadBarsFromTS = session.prepare(
    """                           select
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
                                        disp,
                                        log_co
                                   from mts_bars.bars
                                  where
                                        ticker_id     = :p_ticker_id and
                                        bar_width_sec = :p_width_sec and
                                        ts_begin     >= :p_read_from_ts
                                  allow filtering; """)


  val rowToBar = (row : Row) => {
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
      row.getInt("ticks_cnt"),
      row.getDouble("disp"),
      row.getDouble("log_co")
    )
  }

  case class TickersWidth(ticker_id :Int,bar_width_sec :Int) {

    val maxTsEnd :Long = JavaConverters.asScalaIteratorConverter(session.execute(prepMaxTsEndFuture.bind()
      .setInt("p_ticker_id",ticker_id)
      .setInt("p_bar_width_sec",bar_width_sec))
      .all().iterator())
      .asScala.toSeq.map(r => r.getLong("max_ts_end"))
      .toList
      .headOption.getOrElse(0)

    val seqBars =  JavaConverters.asScalaIteratorConverter(session.execute(prepReadBarsFromTS.bind()
      .setInt("p_ticker_id", ticker_id)
      .setInt("p_width_sec", bar_width_sec)
      .setLong("p_read_from_ts", maxTsEnd))
      .all().iterator())
      .asScala.toSeq.map(row => new BarC(
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
      row.getInt("ticks_cnt"),
      row.getDouble("disp"),
      row.getDouble("log_co")
    )).toList.sortBy(_.ts_begin).toSeq

    val maxTsEndBars = seqBars.map(sb => sb.ts_end).reduceOption(_ max _).getOrElse(0.toLong)

    alogger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    alogger.info("           ")
    alogger.info("  TickersWidth - ticker_id    = "+ticker_id+" widthSec="+bar_width_sec)
    alogger.info("  TickersWidth - maxTsEnd     = "+maxTsEnd)
    alogger.info("  TickersWidth - maxTsEndBars = "+maxTsEndBars+" DiffSec="+(maxTsEndBars-maxTsEnd)+" sec.")
    alogger.info("           ")
    alogger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")

    def getMaxTsEndBars = { maxTsEndBars }

    val diffMaxTsEndBarsSeq_MINUS_MaxTsEndFA = getMaxTsEndBars  - maxTsEnd

    def getMaxTsEnd = maxTsEnd

    def getSeqBars = seqBars

  }








  val prepInsertFutureAnalyze = session.prepare(
    """
      insert into mts_bars.bars_future(
      	ticker_id,
      	bar_width_sec,
      	ts_end,
      	c,
      	ft_log_0017_cls_price,
      	ft_log_0017_res,
      	ft_log_0017_ts_end,
      	ft_log_0034_cls_price,
      	ft_log_0034_res,
      	ft_log_0034_ts_end,
      	ft_log_0051_cls_price,
      	ft_log_0051_res,
      	ft_log_0051_ts_end
      ) values(
      	:p_ticker_id,
      	:p_bar_width_sec,
      	:p_ts_end,
      	:p_c,
      	:p_ft_log_0017_cls_price,
      	:p_ft_log_0017_res,
      	:p_ft_log_0017_ts_end,
      	:p_ft_log_0034_cls_price,
      	:p_ft_log_0034_res,
      	:p_ft_log_0034_ts_end,
      	:p_ft_log_0051_cls_price,
      	:p_ft_log_0051_res,
      	:p_ft_log_0051_ts_end
      );
    """)


  def rowToAdviser (row : Row) = {
    new Adviser(
                row.getInt("adviser_id"),
                row.getInt("ticker_id"),
                row.getString("func_name")
               )
  }

  def rowToCurrTicker (row: Row) = {
    new CurrTicker(
               row.getInt("ticker_id"),
               row.getString("ticker_code"),
               row.getString("ticker_first"),
              row.getString("ticker_seconds")
             )
  }

  val prepDistinctDateTickers = session.prepare(""" select distinct ddate,ticker_id FROM mts_src.ticks where ticker_id=:p_ticker_id allow filtering; """)

  /**
    * Return max(ddate) by ticker
    */
  def getLastTickDdate(p_ticker : Int) ={
    //val rsList = session.execute("select distinct ddate,ticker_id FROM mts_src.ticks;").all()
    val rsList = session.execute(prepDistinctDateTickers.bind().setInt("p_ticker_id", p_ticker)).all()

    //alogger.info(" getLastTickDdate rsList.size="+rsList.size)
    val res = for(i <- 0 to rsList.size()-1) yield
      (
        //rsList.get(i).getInt("ticker_id"),
        rsList.get(i).getDate("ddate"),
        new Date(rsList.get(i).getDate("ddate").getMillisSinceEpoch)
      )

    //#Debug
    /*
    for(r <- res){
      alogger.info(" ---------- r-res - ??? p_ticker="+p_ticker+"  "+r._1+"  "+r._2+"  "+r._3)
    }
    */

    //alogger.info("getLastTickDdate BEFORE max_dates = res.find")

    //BUG !!!  getLastTickDdate_Res_Max searched by all tickers!
    //OLD CODE val getLastTickDdate_Res_Max = {if (res.nonEmpty) res.map(x => x._3).max else 0 }
    // from 31.08.2018 new code
    val getLastTickDdate_Res_Max = {if (res.nonEmpty) res/*.filter(r => r._1 == p_ticker)*/.map(x => x._2).max else 0 }
    val max_dates = res.find(x => x._2 == getLastTickDdate_Res_Max /*&&  x._1 == p_ticker*/).map(x => (x._1,x._2))
    //alogger.info(" >>>>>>>>>>>>>>    [getLastTickDdate] p_ticker="+p_ticker+"  max_dates=["+max_dates+"]")
    max_dates
  }

  /**
    * Return max(ts),max(ts) as UNixTimeStamp by ticker
    */
  def getLastTickTs(p_ticker : Int, p_maxdate : LocalDate) = {
    alogger.info(" >>>  INSIDE getLastTickTs: p_ticker="+p_ticker+" setDate -> p_maxdate="+p_maxdate)
    val bound = prepared.bind().setInt("tickerId", p_ticker).setDate("maxDDate",p_maxdate)
    val rsList = session.execute(bound).all()
    val res = for(i <- 0 to rsList.size()-1) yield new pair_ts_tsunx(
      rsList.get(i).getLong("ts"), //getTimestamp("ts"),
      rsList.get(i).getLong("tsunx")
    )
    alogger.info(" > p_ticker="+p_ticker+" getLastTickTs res(0)  ts="+res(0).ts+"  ts_unx="+res(0).ts_unx)
    res(0)
  }

  val rowToTicker = (row : Row) => {
    val dd = getLastTickDdate(row.getInt("ticker_id"))
    dd match {
      case Some(s) => {

        val l_ticker_id = row.getInt("ticker_id")

        //alogger.info("INSIDE Some(s) l_ticker_id="+l_ticker_id+" s._1="+s._1+"  s._2="+s._2)

        val last_tick_ddate = s._2
        val last_tisk_tss : pair_ts_tsunx = getLastTickTs(l_ticker_id/*row.getInt("ticker_id")*/,s._1)

        //alogger.info("AFTER last_tisk_tss (last_tisk_tss) ts="+last_tisk_tss.ts+"  ts_unx="+last_tisk_tss.ts_unx)

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
          last_tick_ts     = 0,
          last_tick_ts_unx = 0
        )
    }
  }

  //def getTickersList ={
  val seqCurrTickers = {
    JavaConverters.asScalaIteratorConverter(session.execute(prepListTickers.bind()).all().iterator())
      .asScala.toSeq.map(adv => rowToCurrTicker(adv))
  }

  def getCurrTickerByID(p_ticker_id: Int) = {
    seqCurrTickers.filter(t => t.ticker_id==p_ticker_id).head
  }

}
