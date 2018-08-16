package bar.adviser

import java.util.Date

import bar.calculator.BarC
import bar.{ReadCassandraExamples, rowToX}
import com.datastax.driver.core.{Row, Session}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters

case class TendBarsMnMx(ticker_id : Int, width_sec : Int, deep_sec :Int, bars_cnt :Long, ts_begin :Long, ts_end :Long){
  val intervalBeginEndSec = ts_end - ts_begin
  val fromTs = ts_end - deep_sec

  override def toString = {
    "ticker_id =" +ticker_id +" width_sec="+ width_sec +" deep_sec="+ deep_sec +" bars_cnt="+ bars_cnt +" ts_begin="+ ts_begin +" ts_end="+ ts_end +" interval_begin_end_sec="+ intervalBeginEndSec+ "sec.  SPLIT from_ts="+fromTs
  }
}

class TendAdviser(session: Session) extends rowToX(session, LoggerFactory.getLogger(ReadCassandraExamples.getClass)) {

  val logger = LoggerFactory.getLogger(ReadCassandraExamples.getClass)

  /**
    *
    * @param session
    * @param p_ticker_id
    * @param p_width_sec
    * @param p_deep_sec
    * @return
    */
  def getBarsMinMaxTickTendConf(session: Session, p_ticker_id :Int, p_width_sec : Int, p_deep_sec :Int) : Seq[TendBarsMnMx] = {
    val rowToTendBarsMinMax = (row: Row) => {
      TendBarsMnMx(
        p_ticker_id, p_width_sec, p_deep_sec,
        row.getLong("bars_cnt"),
        row.getLong("ts_begin"),
        row.getLong("ts_end"))}
    val bond_query_ =  prepQuery.bind().setInt("p_ticker_id", p_ticker_id)
      .setInt("p_width_sec", p_width_sec)
    val rsTendBarsMnMx :Seq[TendBarsMnMx] = JavaConverters.asScalaIteratorConverter(session.execute(bond_query_).all().iterator())
      .asScala.toSeq.map(rowToTendBarsMinMax)
    rsTendBarsMnMx
  }

  def simpleRound6Double(valueD : Double) = {
    (valueD * 1000000).round / 1000000.toDouble
  }

  /**
    *
    * @param session
    * @param p_ticker_id
    * @param p_width_sec
    * @param p_ts_begin
    * @param p_ts_end
    * @return
    */
  def getBarsByTsInterval(session: Session, p_ticker_id :Int, p_width_sec : Int, p_ts_begin :Long, p_ts_end :Long) : Seq[BarC] = {
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
      )}

    logger.debug("---------------------------------------")
    logger.debug("")
    logger.debug("   p_ts_begin="+p_ts_begin+"   p_ts_end="+p_ts_end)
    logger.debug("")
    logger.debug("---------------------------------------")

    val bondBarsQuery =  prepQueryBars.bind().setInt("p_ticker_id", p_ticker_id)
      .setInt("p_width_sec", p_width_sec)
      .setLong("p_ts_begin",p_ts_begin)
      .setLong("p_ts_end",p_ts_end)
    val rsBars :Seq[BarC] = JavaConverters.asScalaIteratorConverter(session.execute(bondBarsQuery).all().iterator())
      .asScala.toSeq.map(rowToBar).sortBy(t => t.ts_end)
    rsBars
  }


  /**
    *
    * @param p_seqBarInfo - Full seq of info
    * @param p_index      - can be: 0,1,2 - only 3 parts
    * @param p_btype      - p_btype can be: g,r,n
    */
  def getBtypeCntByTypeIndex(p_ticker :Int, p_seqBarInfo : List[(Int,Map[String,(Int,Double)])], p_index :Int, p_btype :String) = {
    val res = p_seqBarInfo.filter(sb => sb._1 == p_ticker)(p_index)._2.getOrElse(p_btype,(0,0.toDouble))._1
    logger.debug(" INSIDE   []   :  p_ticker = "+p_ticker+"  index = "+p_index+" btype = "+p_btype+"   CNT = "+res )
    res
  }

  def getLogCoByTypeIndex(p_ticker :Int, p_seqBarInfo : List[(Int,Map[String,(Int,Double)])], p_index :Int, p_btype :String) = {
    val res = p_seqBarInfo.filter(sb => sb._1 == p_ticker)(p_index)._2.getOrElse(p_btype,(0,0.toDouble))._2
    logger.debug(" INSIDE   []   :  p_ticker = "+p_ticker+"  index = "+p_index+" btype = "+p_btype+"   LOG_CO = "+res )
    res
  }

  def getSimpleBarsCntByIndex(pTicker :Int, seqParts : List[Seq[BarC]], pIndex :Int) =
    seqParts.filter(sb => sb.head.ticker_id == pTicker)(pIndex).size



  def calc()  = {
    logger.info("BEGIN TendAdviser calc()")

    val adv_width_sec    :Int = 600
    val adv_deep_sec     :Int = 3*3600
    val adv_bars_in_part :Int = 6

    val p1_seq_crit :Seq[Int] = Seq(adv_bars_in_part/3, adv_bars_in_part/2)
    val p2_seq_crit :Seq[Int] = Seq(adv_bars_in_part/2, adv_bars_in_part*2/3)
    val p3_seq_crit :Seq[Int] = Seq(adv_bars_in_part*2/3)

    val tickersWWRes =  JavaConverters.asScalaIteratorConverter(session.execute(prepqueryTickersWW.bind()).all().iterator())
      .asScala.toSeq.map(r => (r.getInt("ticker_id"), adv_width_sec, adv_deep_sec)).sortBy(_._1).toList

    val tendRes : Seq[TendBarsMnMx] = {for {
      (lTickerId, l_width_sec, l_deep_sec) <- tickersWWRes
      thisTendRes : TendBarsMnMx  <- getBarsMinMaxTickTendConf(session, lTickerId, l_width_sec, l_deep_sec)
    } yield thisTendRes
    }.filter(r => (r.intervalBeginEndSec >= r.deep_sec))

    logger.info(" tendRes: ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    for (tr <- tendRes) logger.info("    tendRes  = "+tr.toString)
    logger.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")

    val barsListSrc : Seq[BarC] = for {
      barMnMx        <- tendRes
      thisBar : BarC <- getBarsByTsInterval(session, barMnMx.ticker_id, barMnMx.width_sec, barMnMx.fromTs, barMnMx.ts_end)
    } yield thisBar

    for (b <- barsListSrc) logger.debug(" barsListSrc =   ticker_id="+b.ticker_id+" begin - end :    "+b.ts_begin+" - "+b.ts_end+"  ["+b.btype+"] "+b.log_co)

    val barsListFilteredOnCount : Seq[BarC]  = barsListSrc.filter(bl => (barsListSrc.count(b => b.ticker_id==bl.ticker_id) == 3*adv_bars_in_part))

    //Here we need filter to take only tickers where bars count = adv_bars_in_part(6)*3
    logger.info("barsListFilteredOnCount.size="+barsListFilteredOnCount.size)
    for (b <- barsListFilteredOnCount) logger.info(" barsListFilteredOnCount =   ticker_id="+b.ticker_id+" begin - end :    "+b.ts_begin+" - "+b.ts_end+"  ["+b.btype+"] "+b.log_co)

    val seqSeqBars_Parts = barsListFilteredOnCount.sliding(adv_bars_in_part,adv_bars_in_part).toList.filter(oneSeq => oneSeq.size == adv_bars_in_part)

    logger.info(" seqSeqBars_Parts :################################# seqSeqBars_Parts.size="+seqSeqBars_Parts.size)
    for (b <- seqSeqBars_Parts){
      logger.info("ticker_id="+b.head.ticker_id+" parts : size="+b.size+" begin-end: "+b.head.ts_begin+"  "+b.last.ts_end)
    }
    logger.info("####################################################")

    val seqSeqBars_Parts_AddInfo = for (blck <- seqSeqBars_Parts) yield {
      logger.info("      seqSeqBars_Parts_AddInfo > ticker_id = "+blck.head.ticker_id)
      (blck.head.ticker_id,
        Map(
          ("g",(blck.count(b => b.btype=="g"), simpleRound6Double(blck.filter(b => b.btype=="g").map(b => b.log_co).sum) )),
          ("r",(blck.count(b => b.btype=="r"), simpleRound6Double(blck.filter(b => b.btype=="r").map(b => b.log_co).sum) )),
          ("n",(blck.count(b => b.btype=="n"), simpleRound6Double(blck.filter(b => b.btype=="n").map(b => b.log_co).sum) ))
        ))
    }

    logger.info("---------------------------------------")
    logger.info("!!!!!!!!!!!  seqSeqBars_Parts_AddInfo.size="+seqSeqBars_Parts_AddInfo.size)
    logger.info("---------------------------------------")

    for (p <- seqSeqBars_Parts_AddInfo){
      logger.info(" [seqSeqBars_Parts_AddInfo]   ticker_id="+p._1+"  "+p.toString)
    }


    val wType = for (cticker <- seqSeqBars_Parts_AddInfo.map(bi => bi._1).distinct.sortBy(v => v)) yield {
      //1.Test on G.(Green - up bars)
      if (
        (p1_seq_crit contains getBtypeCntByTypeIndex(cticker, seqSeqBars_Parts_AddInfo, 0, "g")) &&
          (p2_seq_crit contains getBtypeCntByTypeIndex(cticker, seqSeqBars_Parts_AddInfo, 1, "g")) &&
          (p3_seq_crit contains getBtypeCntByTypeIndex(cticker, seqSeqBars_Parts_AddInfo, 2, "g"))
          &&
          (getLogCoByTypeIndex(cticker, seqSeqBars_Parts_AddInfo, 2, "g") >
            getLogCoByTypeIndex(cticker, seqSeqBars_Parts_AddInfo, 1, "g")) &&
          (getLogCoByTypeIndex(cticker, seqSeqBars_Parts_AddInfo, 1, "g") >
            getLogCoByTypeIndex(cticker, seqSeqBars_Parts_AddInfo, 0, "g"))
      ) {
        logger.debug("GREEN WAY")
        (cticker,"g")
      }
      //2. Test on R.(Red - down bars)
      else if (
        (p1_seq_crit contains getBtypeCntByTypeIndex(cticker, seqSeqBars_Parts_AddInfo, 0, "r")) &&
          (p2_seq_crit contains getBtypeCntByTypeIndex(cticker, seqSeqBars_Parts_AddInfo, 1, "r")) &&
          (p3_seq_crit contains getBtypeCntByTypeIndex(cticker, seqSeqBars_Parts_AddInfo, 2, "r"))
          &&
          (getLogCoByTypeIndex(cticker, seqSeqBars_Parts_AddInfo, 2, "r") <
            getLogCoByTypeIndex(cticker, seqSeqBars_Parts_AddInfo, 1, "r")) &&
          (getLogCoByTypeIndex(cticker, seqSeqBars_Parts_AddInfo, 1, "r") <
            getLogCoByTypeIndex(cticker, seqSeqBars_Parts_AddInfo, 0, "r"))
      ) {
        logger.debug("RED WAY")
        (cticker,"r")
      }
      else {
        logger.debug("ELSE WAY")
        (cticker,"n")
      }
    }

    for (tr <- wType if Seq("g","r") contains tr._2){
      logger.info("RES w="+tr._2+" ticker_id = "+tr._1+" deep_sec="+adv_deep_sec+" adv_bars_in_part="+adv_bars_in_part+
        "   ts_res="+barsListFilteredOnCount.filter(bl => bl.ticker_id==tr._1).map(bl => bl.ts_end).max+
        "   p1_size = "+getSimpleBarsCntByIndex(tr._1,seqSeqBars_Parts,0)+
        "   p2_size = "+getSimpleBarsCntByIndex(tr._1,seqSeqBars_Parts,1)+
        "   p3_size = "+getSimpleBarsCntByIndex(tr._1,seqSeqBars_Parts,2)+
        "   p1_cnt = "+getBtypeCntByTypeIndex(tr._1, seqSeqBars_Parts_AddInfo, 0, tr._2)+
        "   p2_cnt = "+getBtypeCntByTypeIndex(tr._1, seqSeqBars_Parts_AddInfo, 1, tr._2)+
        "   p3_cnt = "+getBtypeCntByTypeIndex(tr._1, seqSeqBars_Parts_AddInfo, 2, tr._2)+
        "   p1_logco = "+getLogCoByTypeIndex(tr._1, seqSeqBars_Parts_AddInfo, 0, tr._2)+
        "   p2_logco = "+getLogCoByTypeIndex(tr._1, seqSeqBars_Parts_AddInfo, 1, tr._2)+
        "   p3_logco = "+getLogCoByTypeIndex(tr._1, seqSeqBars_Parts_AddInfo, 2, tr._2))

      val boundInsertRes = prepInsertRes.bind()
        .setInt("ticker_id", tr._1)
        .setInt("bar_width_sec", adv_width_sec)
        .setLong("ts_res", barsListFilteredOnCount.filter(bl => bl.ticker_id == tr._1).map(bl => bl.ts_end).max)
        .setString("way", tr._2.toString)
        .setInt("deep_sec", adv_deep_sec)
        .setInt("adv_bars_in_part", adv_bars_in_part)
        .setInt("p1_size_bars", getSimpleBarsCntByIndex(tr._1, seqSeqBars_Parts, 0))
        .setInt("p2_size_bars", getSimpleBarsCntByIndex(tr._1, seqSeqBars_Parts, 1))
        .setInt("p3_size_bars", getSimpleBarsCntByIndex(tr._1, seqSeqBars_Parts, 2))
        .setInt("p1_cnt", getBtypeCntByTypeIndex(tr._1, seqSeqBars_Parts_AddInfo, 0, tr._2))
        .setInt("p2_cnt", getBtypeCntByTypeIndex(tr._1, seqSeqBars_Parts_AddInfo, 1, tr._2))
        .setInt("p3_cnt", getBtypeCntByTypeIndex(tr._1, seqSeqBars_Parts_AddInfo, 2, tr._2))
        .setDouble("p1_logco", getLogCoByTypeIndex(tr._1, seqSeqBars_Parts_AddInfo, 0, tr._2))
        .setDouble("p2_logco", getLogCoByTypeIndex(tr._1, seqSeqBars_Parts_AddInfo, 1, tr._2))
        .setDouble("p3_logco", getLogCoByTypeIndex(tr._1, seqSeqBars_Parts_AddInfo, 2, tr._2))
      session.execute(boundInsertRes)
    }

    logger.info("END TendAdviser calc()")
  }


}
