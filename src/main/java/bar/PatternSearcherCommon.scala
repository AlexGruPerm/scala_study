package bar

import java.util.Date

import bar.calculator._
import com.datastax.driver.core.Session
import org.slf4j.Logger

import scala.collection.JavaConverters

/**Common class for PatternSearcher with internal classes and session.prepare queries.
  *
  * @param session - cassandr session to db.
  * @param alogger - instance of LoggerFactory.getLogger(ReadCassandraExamples.getClass)
  */
abstract class PatternSearcherCommon(val session: Session,val alogger: Logger) {

  val prepTickersWidths = session.prepare(""" select ticker_id,bar_width_sec from mts_meta.bars_property """)

  val prepReadBarsAll = session.prepare(
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
                                        bar_width_sec = :p_width_sec
                                  allow filtering; """)

  val prepResFromFutAnalyze = session.prepare(""" select * from mts_bars.bars_future where ticker_id=:p_ticker_id and bar_width_sec=:p_width_sec and ts_end in :ts_end_collect; """)

  val prepSavePatSearchRes = session.prepare(
    """
      insert into mts_bars.pattern_search_results(
          ticker_id,
          bar_width_sec,
          patt_ts_begin,
          patt_ts_end,
          patt_end_c,
          patt_bars_count,
          history_found_tsends,
          ft_log_0017_res_u,
          ft_log_0017_res_d,
          ft_log_0017_res_n,
          ft_log_0034_res_u,
          ft_log_0034_res_d,
          ft_log_0034_res_n,
          ft_log_0051_res_u,
          ft_log_0051_res_d,
          ft_log_0051_res_n,
          ft_log_sum_u,
          ft_log_sum_d,
          ft_log_sum_n)
   values(
          :p_ticker_id,
          :p_bar_width_sec,
          :p_patt_ts_begin,
          :p_patt_ts_end,
          :p_patt_end_c,
          :p_patt_bars_count,
          :p_history_found_tsends,
          :p_ft_log_0017_res_u,
          :p_ft_log_0017_res_d,
          :p_ft_log_0017_res_n,
          :p_ft_log_0034_res_u,
          :p_ft_log_0034_res_d,
          :p_ft_log_0034_res_n,
          :p_ft_log_0051_res_u,
          :p_ft_log_0051_res_d,
          :p_ft_log_0051_res_n,
          :p_ft_log_sum_u,
          :p_ft_log_sum_d,
          :p_ft_log_sum_n
          );
    """)

  //----------------------------------------------------------------------------------
  //Current pattern
  case class PatternForSearchCls(seqBarsSrc : Seq[BarC]) {
    val sum_tick_cnt = seqBarsSrc.map(b => b.ticks_cnt).sum
    val sum_abs_logco = seqBarsSrc.map(b => Math.abs(b.log_co)).sum

    val seqBars: Seq[BarCPat] = seqBarsSrc.map(
      b => BarCPat(b,
        (b.ticks_cnt * 100 / sum_tick_cnt.toDouble),
        (b.ticks_cnt * 100 / sum_tick_cnt.toDouble) * 0.8,
        (b.ticks_cnt * 100 / sum_tick_cnt.toDouble) * 1.2,
        (Math.abs(b.log_co) * 100 / sum_abs_logco),
        (Math.abs(b.log_co) * 100 / sum_abs_logco) * 0.7,
        (Math.abs(b.log_co) * 100 / sum_abs_logco) * 1.3
      )
    )

    def getSeqBarCpat = {
      seqBars
    }

    override def equals(that: Any): Boolean ={
      that match {
        case that: PatternForSearchCls => {
          // this - current instance
          // that - incoming instance
          if (this.seqBars.size == that.seqBars.size) {
            //alogger.info(" this.seqBars.head.b.ts_end="+this.seqBars.head.b.ts_end+"  that.seqBars.head.b.ts_end="+ that.seqBars.head.b.ts_end)
            val zipSeq = that.seqBars.zip(this.seqBars)
            if ((zipSeq.filter(z => z._1.b.btype == z._2.b.btype).size == zipSeq.size) &&
              (zipSeq.filter(z => (z._1.ticks_cnt_prcnt >= z._2.ticks_cnt_prcnt_from && z._1.ticks_cnt_prcnt <= z._2.ticks_cnt_prcnt_to)).size == zipSeq.size) &&
              (zipSeq.filter(z => (z._1.abs_logco_prcnt >= z._2.abs_logco_prcnt_from && z._1.abs_logco_prcnt <= z._2.abs_logco_prcnt_to)).size >= zipSeq.size-1)

            /*
             (that.seqBars(0).abs_logco_prcnt >= this.seqBars(0).abs_logco_prcnt_from && that.seqBars(0).abs_logco_prcnt <= this.seqBars(0).abs_logco_prcnt_to)
             (that.seqBars(0).ticks_cnt_prcnt >= this.seqBars(0).ticks_cnt_prcnt_from && that.seqBars(0).ticks_cnt_prcnt <= this.seqBars(0).ticks_cnt_prcnt_to) &&
            */
            )
              true
            else
              false
          } else
            false
        }
        case _ => false
      }
    }


  }




  // For PatternSeacher
  case class TickerProperty(ticker_id :Int, bar_width_sec :Int,deepCntPattern :Int){

    val seqBars =  JavaConverters.asScalaIteratorConverter(session.execute(prepReadBarsAll.bind()
      .setInt("p_ticker_id", ticker_id)
      .setInt("p_width_sec", bar_width_sec))
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
    //alogger.info("LAST:"+seqBars.last.ts_end)

    val patternForSearch = if (seqBars.nonEmpty)
      new PatternForSearchCls(seqBars.slice(seqBars.size-deepCntPattern, seqBars.size-1) :+ seqBars.last)
    else None

    val searchArea : Seq[BarC] = patternForSearch match {
      case pfs:PatternForSearchCls => seqBars.takeWhile(b => b.ts_end < pfs.seqBarsSrc.head.ts_end)
      case None => Nil
    }

    //val searchArea : Seq[BarC] = seqBars.takeWhile(b => b.ts_end < patternForSearch.seqBarsSrc.head.ts_end)

    //последовательность кусочков, баров - каждый из которых сравниваем с текущим и проверяем удовлетворяет ли он условиям
    // сравнения!
    def getSearchResult :Seq[Seq[BarC]] = {
      val res = if (searchArea.nonEmpty) {
        patternForSearch match {
          case pfs:PatternForSearchCls => {
            val seqOfSeq = for (i <- 0.to(searchArea.size - pfs.getSeqBarCpat.size)) yield {
              //alogger.info("DEVIDE PARTS from "+i+" to "+(i+patternForSearch.getSeqBarCpat.size-1))
              searchArea.slice(i, (i + pfs.getSeqBarCpat.size - 1)) :+ searchArea(i + pfs.getSeqBarCpat.size - 1)
            }
            seqOfSeq.filter(sb => pfs == new PatternForSearchCls(sb))
          }
          case None => Nil
        }
      } else
        Nil
      res
    }


    def getFutAnalResultsByTsEnds(tsEndCollect : java.util.List[java.lang.Long]/*List[java.lang.Long]*/) /*:Seq[BarFutAnalRes]*/ = {
      //Use ticker_id :Int, bar_width_sec :Int, from Class TickerProperty

      JavaConverters.asScalaIteratorConverter(session.execute(prepResFromFutAnalyze.bind()
        .setInt("p_ticker_id", ticker_id)
        .setInt("p_width_sec", bar_width_sec)     //, TypeToken.of(classOf[java.lang.Long]
        .setList("ts_end_collect", tsEndCollect) // java.util.Arrays.asList(tsEndCollect) // java.lang.Long
      )                                                         // TypeToken.of(classOf[java.lang.Integer])
        .all().iterator())
        .asScala.toSeq.map(row => new BarFutAnalRes(
        row.getInt("ticker_id"),
        row.getInt("bar_width_sec"),
        row.getLong("ts_end"),
        row.getDouble("c"),
        //-----------------------------------
        row.getLong("ft_log_0017_ts_end"),
        row.getString("ft_log_0017_res"),
        row.getDouble("ft_log_0017_cls_price"),
        //-----------------------------------
        row.getLong("ft_log_0034_ts_end"),
        row.getString("ft_log_0034_res"),
        row.getDouble("ft_log_0034_cls_price"),
        //-----------------------------------
        row.getLong("ft_log_0051_ts_end"),
        row.getString("ft_log_0051_res"),
        row.getDouble("ft_log_0051_cls_price")
      )
      ).toList.sortBy(_.ts_end).toSeq
    }

  }




}
