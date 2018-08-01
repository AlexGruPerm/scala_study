package bar.adviser

import bar.{ReadCassandraExamples, rowToX}
import bar.calculator._
import bar.calculator.BarCalculator
import java.text.SimpleDateFormat
import java.util.Date
import com.datastax.driver.core
import com.datastax.driver.core.{LocalDate, Row, Session}
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters

class TradeAdviser(session: Session) extends rowToX(session, LoggerFactory.getLogger(ReadCassandraExamples.getClass)) {

  val logger = LoggerFactory.getLogger(ReadCassandraExamples.getClass)

  def getAdviserList() = {
    JavaConverters.asScalaIteratorConverter(session.execute(resListAdvisers.bind()).all().iterator())
                               .asScala.toSeq.map(adv => rowToAdviser(adv))
  }





  //--------------------------------------------------------------------------------------------------------------
  def trd_adv_1_simple()={
   logger.info("  INSIDE trd_adv_1_simple")
    /*
    1  - EURUSD
    -----------
    5  - EURCHF
    8  - EURCAD
    12 - EURGBP
    -----------
    */
   val barsLimit  = 3.toInt
   val mainTicker = 1.toInt // ID of main ticker
   val listTickers = Seq(1,5,8,12) //list of main and companion tickers.
    logger.info("Main [" + mainTicker + "] symbol = " + getCurrTickerByID(mainTicker).ticker_code)
    val seqSeqBar :Seq[Seq[BarC]] = for (compTicker <- listTickers) yield {
                                      logger.debug("Companion ["+compTicker+"] symbol = "+getCurrTickerByID(compTicker).ticker_code)
                                      logger.debug(" BIND compTicker="+compTicker+" plimit="+barsLimit)
                                      val bound = resNLastBars.bind().setInt("tickerId", compTicker)
                                                                     .setInt("plimit", barsLimit)
                                      val rsBar = session.execute(bound).all()
                                      val barsByThisTicker = for(i <- 0 to rsBar.size()-1) yield rowToBar(rsBar.get(i))
                                      logger.debug("barsByThisTicker.size="+barsByThisTicker.size+"  -  "+barsByThisTicker)
                                      barsByThisTicker
                                    }

    logger.info("             ")
    for (debugTicker <- Seq(1,5,8,12)) {
      logger.info("-ticker = " + debugTicker + " [" + getCurrTickerByID(debugTicker).ticker_code + "]  ===================")
      for (sb <- seqSeqBar.flatten.filter(sb => sb.ticker_id == debugTicker)) {
        logger.info("[" + sb.ticker_id + "]   TSBEGIN=" + sb.ts_begin + " TSEND=" + sb.ts_end + " TSEND_UNX=" + sb.ts_end_unx + " BTYPE=" + sb.btype + " C=" + sb.c)
      }
      // !!!!! HEAD it's a first element in Seq !!!!!
      //logger.info("head ! = "+seqSeqBar.flatten.filter(sb => sb.ticker_id == debugTicker).head.ts_end_unx)
      logger.info("------------------------------------------")
    }

  }
  //--------------------------------------------------------------------------------------------------------------






  def calc()  = {
    logger.info("BEGIN TradeAdviser calc()")
    val adviserList : Seq[Adviser] = getAdviserList()
    logger.info("LOADED adviserList.size="+adviserList.size)
     for (adviser <- adviserList){
       logger.info("ADVISER: adviser_id"+adviser.adviser_id+" ticker_id="+adviser.ticker_id+" func="+adviser.func_name)
      val res = {
        adviser.func_name match
       {
         case "trd_adv_1_simple" => trd_adv_1_simple()
         case _ => logger.info("any adviser function!")
       }
      }


     }


  }

}


