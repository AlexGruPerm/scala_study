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


  def getDatesByTickers(listTickers : Seq[Int], p_bar_width : Int)  = {
    /**
      * val boundLastDate = resLastDateBar.bind().setInt("tickerId", bp.ticker_id)
      * .setInt("barWidth", bp.bar_width_sec)
      * val rsLastDate = session.execute(boundLastDate).one()
      * new barProps(bp.ticker_id, rsLastDate.getDate("ddate"), bp.bar_width_sec)
       */
      val res = for(tick <- listTickers) yield {
        val OptMaxDate = try {
          val boundLastDate = resLastDateBar.bind().setInt("tickerId", tick)
            .setInt("barWidth", p_bar_width)
          val rsLastDate = session.execute(boundLastDate).one()
          if (rsLastDate.getDate("ddate") != null)
            Some(rsLastDate.getDate("ddate"))
          else
            None
        } catch {
          case e: Exception => {
            logger.info("ERROR: getDatesByTickers - no bars calculated -"+e.getMessage)
            None
          }
        }
        (tick,OptMaxDate)
      }
    res
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
   val p_adviser_id = 1
   val barsWidth = 600
   val barsLimit  = 2.toInt
   val mainTicker = 1.toInt // ID of main ticker
   val listTickers = Seq(1,5,8,12) //list of main and companion tickers.
    logger.info("Main [" + mainTicker + "] symbol = " + getCurrTickerByID(mainTicker).ticker_code)

   //flatten remove all None elements from Seq of Option and stay only Same
   val max_dates = getDatesByTickers(listTickers,barsWidth)

    logger.info("max_dates.flatteen.size="+max_dates.size)

    for (md <- max_dates) {
      logger.info("md="+md._1+" ddate="+md._2)
    }

    val seqSeqBar :Seq[Seq[BarC]] = for (compTicker <- max_dates.withFilter(md => md._2!=None).map(mdt => mdt._1)/*compTicker <- listTickers*/) yield {
                                      logger.debug("Companion ["+compTicker+"] symbol = "+getCurrTickerByID(compTicker).ticker_code)
                                      logger.debug(" BIND compTicker="+compTicker+" plimit="+barsLimit)

                                      val bound = resNLastBars.bind().setInt("tickerId", compTicker)
                                                                     .setDate("pddate", max_dates.filter(md => md._1==compTicker).head._2.getOrElse(null))
                                                                     .setInt("p_bar_width_sec",600)
                                                                     .setInt("plimit", barsLimit)

                                      val rsBar = session.execute(bound).all()
                                      val barsByThisTicker = for(i <- 0 to rsBar.size()-1) yield rowToBar(rsBar.get(i))
                                      logger.debug("barsByThisTicker.size="+barsByThisTicker.size+"  -  "+barsByThisTicker)
                                      barsByThisTicker
                                    }


/**
    logger.info("             ")
    logger.info("  SeqSeq: seqSeqBar.size="+seqSeqBar.size)
    logger.info("             ")
*/

    /**
      *  Here we need check that seqSeqBar contains 4 subSeq, 1 for each ticker_id for this adviser.
      *  And each subseq contains exact N Bars.
      *
      */

    if (seqSeqBar.size==4) {
      logger.info("1. condition check for adviser_id = 1 seqSeqBar.size == 4")
        if ( seqSeqBar.exists(seqTicker => seqTicker.size < barsLimit) ) {
          logger.info("2. there is seq with size less then "+barsLimit)
        } else {
               logger.info("2. there is NO seq with size less then "+barsLimit+" OK. NEXT checking.")
               // Checking types of each bars, comparison.
               // For simplicity split common seq on subseq.
               val seq_1  = seqSeqBar.flatten.filter(sb => sb.ticker_id == 1)
               val seq_5  = seqSeqBar.flatten.filter(sb => sb.ticker_id == 5)
               val seq_8  = seqSeqBar.flatten.filter(sb => sb.ticker_id == 8)
               val seq_12 = seqSeqBar.flatten.filter(sb => sb.ticker_id == 12)

          //   DEBUG OUTPUT
          for (debugTicker <- Seq(1,5,8,12)) {
            logger.info("-ticker = " + debugTicker + " [" + getCurrTickerByID(debugTicker).ticker_code + "]")
            for (sb <- seqSeqBar.flatten.filter(sb => sb.ticker_id == debugTicker)) {
              logger.info("[" + sb.ticker_id + "]   TSBEGIN=" + sb.ts_begin + " TSEND=" + sb.ts_end + " TSEND_UNX=" + sb.ts_end_unx + " BTYPE=" + sb.btype + " C=" + sb.c)
            }
            // !!!!! HEAD it's a first element in Seq !!!!!
            logger.info("-------------------")
          }

          //logger.info(" >>>  seq_1.size="+seq_1.size)
          //logger.info(" >>>  seq_1.filter(b => b.btype=='r').size="+seq_1.count(b => b.btype=="r")+"    seq_1.filter(b => b.btype=='g').size="+seq_1.filter(b => b.btype=="g").size)

              if (seq_1.count(b => b.btype=="r")==barsLimit) {
                logger.info("2.2 Main ticker ["+barsLimit+"R]")
              }

              if (seq_1.count(b => b.btype=="g")==barsLimit) {
                logger.info("2.2 Main ticker ["+barsLimit+"G]")
              }

               if (
                   seq_1.count(b => b.btype=="r")==barsLimit  &&
                     (seq_5.count( b => b.btype=="r")==barsLimit || (seq_5.count(b  => b.btype=="r")==(barsLimit-1) && seq_5.count(b  => b.btype=="n")==1 )) &&
                     (seq_8.count( b => b.btype=="r")==barsLimit || (seq_8.count(b  => b.btype=="r")==(barsLimit-1) && seq_8.count(b  => b.btype=="n")==1 )) &&
                     (seq_12.count(b => b.btype=="r")==barsLimit || (seq_12.count(b => b.btype=="r")==(barsLimit-1) && seq_12.count(b => b.btype=="n")==1 ))
               ) {
                 logger.info("###########################################################")
                 logger.info("                                                           ")
                 logger.info("  3. [R] There are SAME bar types in MAIN ticker. NEXT!!!  ")
                 logger.info("                                                           ")
                 logger.info("###########################################################")

                 val boundSaveAdvRes = prepSaveAdviserRes.bind()
                   .setInt("p_adviser_id", p_adviser_id)
                   .setLong("p_ts", seq_1.map(b => b.ts_end).max)
                   .setString("p_main_way","R - main go down")
                 session.execute(boundSaveAdvRes)

               }
               else if
                 (
                 seq_1.count(b => b.btype=="g")==barsLimit  &&
                   (seq_5.count( b => b.btype=="g")==barsLimit || (seq_5.count(b  => b.btype=="g")==(barsLimit-1) && seq_5.count(b  => b.btype=="n")==1 )) &&
                   (seq_8.count( b => b.btype=="g")==barsLimit || (seq_8.count(b  => b.btype=="g")==(barsLimit-1) && seq_8.count(b  => b.btype=="n")==1 )) &&
                   (seq_12.count(b => b.btype=="g")==barsLimit || (seq_12.count(b => b.btype=="g")==(barsLimit-1) && seq_12.count(b => b.btype=="n")==1 ))
                 )
               {
                 logger.info("###########################################################")
                 logger.info("                                                           ")
                 logger.info("  3. [G] There are SAME bar types in MAIN ticker. NEXT!!!  ")
                 logger.info("                                                           ")
                 logger.info("###########################################################")

                 val boundSaveAdvRes = prepSaveAdviserRes.bind()
                   .setInt("p_adviser_id", p_adviser_id)
                   .setLong("p_ts", seq_1.map(b => b.ts_end).max)
                   .setString("p_main_way","G - main go up")
                 session.execute(boundSaveAdvRes)

               } else {
                 logger.info("3. There are different bar types in MAIN ticker.")
               }
        }
    } else {
      logger.info("No conditions for adviser_id = 1 seqSeqBar.size != 4")
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


