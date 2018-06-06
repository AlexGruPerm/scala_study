
/** presents one tick as object
  *
  * @param Data tick data for constructor
  */
case class Tick(Data : Tuple2[Int,Int]) {
  val tNum : Int = Data._1
  val tVal : Int = Data._2
}


/** presents sequence of ticks (instances of Tick class)
  *
  * @param Data sequence of ticks for constructor
  */
case class TicksDs (Data : Seq[Tick]) {
  def size : Int = Data.size
}


/** For read source file and return necessary data for instantiate TicksDs class object
  *
  * @param CsvPath - path to source CSV file
  */
class scvReader(CsvPath : String){

  def using[A <: { def close(): Unit }, B](param: A)(f: A => B): B =
    try f(param)
      finally param.close()

  /** Read source file line by line, parse it, prepare sequence of ticks and return TicksDs type
    *
    * @return
    */
  def readTicks : TicksDs = {
    using(io.Source.fromFile(CsvPath)) { source =>TicksDs(source.getLines.map{oneLine => (oneLine.split(";")(0).toInt-1,
                                                                                          oneLine.split(";")(1).toInt)}
                                                                          .toList.map(x => Tick(x._1,x._2)))
    }
  }
}


/** Instance of class represent one Bar
  *
  * @param barTicks : Seq[Tick] - Sequence of ticks that define this Bar.
  */
class Bar(barTicks : Seq[Tick]){
  val bNumBegin :Int = barTicks(0).tNum
  val bNumEnd   :Int = barTicks.last.tNum

  val bOpen     :Int = barTicks(0).tVal
  val bHigh     :Int = barTicks.map(x=>x.tVal).max
  val bLow      :Int = barTicks.map(x=>x.tVal).min
  val bClose    :Int = barTicks.last.tVal

  val bWidth    : Int = barTicks.size//bNumEnd - bNumBegin
  val bHighBody : Int = math.abs(bClose-bOpen)
  val bHighShad : Int = math.abs(bHigh-bLow)

  val bType     : String = (bOpen compare bClose).signum match {
                                                        case -1 => "g" // bOpen < bClose
                                                        case  0 => "n" // bOpen = bClose
                                                        case  1 => "r" // bOpen > bClose
                                                      }

  override def toString =
   "[ "+bNumBegin+":"+bNumEnd+"] ohlc=["+bOpen+","+bHigh+","+bLow+","+bClose+"] "+bType+"   body,shad=["+bHighBody+","+bHighShad+"]"

}


case class BarForwardRes(beginBar : Bar,endBar : Option[Bar], hValueHight : Int) {

  val beRes : Tuple2[String,Int] = if (endBar.isDefined) {
                               if (endBar.get.bHigh > beginBar.bClose+hValueHight) ("u" -> (endBar.get.bHigh - (beginBar.bClose+hValueHight)))
                                 else if (endBar.get.bLow < beginBar.bClose-hValueHight) ("d" -> ((beginBar.bClose-hValueHight)-endBar.get.bLow))
                               else ("n"->0)
                             } else ("n"->0)


  val beType   : String = beRes._1
  val beNumRes : Int    = beRes._2
  val resTicksDuration  : Int = endBar.get.bNumBegin - beginBar.bNumEnd

  override def toString = {
    beginBar.toString+" > "+endBar.toString
  }
}


/** presents sequence of bars (instances of Bar class)
  *
  * Additional functionality some compare methods, used in search current pattern in history.
  *
  * @param Data
  */
case class BarsDS(Data : Seq[Bar]){
  def size : Int = Data.size

  def compareByTypes(that: BarsDS) : Boolean= {
    require(Data.size == that.Data.size, "[compareByTypes] Compared sequences has to be same size")
    if (Data.zip(that.Data).filter(barsPair => (barsPair._1.bType != barsPair._2.bType)).nonEmpty /*.size != 0*/) false
    else
      true
  }
}


/** Make sequence of Bar (instance BarsDS) from sequence of Tick (instance TicksDs)
  *
  * @param ticks        - Source sequence of ticks (instances of class Tick)
  * @param ticksCntBar  - Ticks count in one Bar
  */
class BarBuilder(ticks : TicksDs, ticksCntBar : Int){
  def getBars : BarsDS = new BarsDS(for(oneBar <- ticks.Data.sliding(ticksCntBar,ticksCntBar).filter(x => (x.size==ticksCntBar)).toSeq) yield
                                      new Bar(oneBar))

}


/**  In the history of bars (object bars) search current formation (patter) - object barsCurr
  *    and return all !-LAST-! bars of founded formations as an object of class BarsDS
  */
class PatterSearcher(barsHist : BarsDS, barsCurr :BarsDS){

  def patterSearchHistory : BarsDS =
    new BarsDS(barsHist.Data.sliding(barsCurr.size,barsCurr.size).filter(bh => (bh.size == barsCurr.size))
                                                                 .filter(bh =>  (new BarsDS(bh)).compareByTypes(barsCurr))
                                                                 .map(bh => bh.last).toSeq)

}


/** Class just for visualization pattern search
  *
  * @param barsHist  - Full history bars
  * @param barsCurr  - Bars in current pattern
  * @param barsFound - result of search current pattern in history, last bars of patterns.
  */
class VisualSearchResults(barsHist : BarsDS, barsCurr :BarsDS, barsFound: BarsDS){
  def show = {
    println("   ")
    println(" VISUAL : barsHist=["+barsHist.size+"] barsCurr=["+barsCurr.size+"] barsFound=["+barsFound.size+"]")
    println("   ")
     for (bc <- barsCurr.Data) println(bc)
    println("   ")

    for (bh <- barsHist.Data){
      print(bh+" ")
      for (bf <- barsFound.Data){
        if (bh == bf)
           print(" in")
      }
      println(" ")
    }
    println("   ")
    println("   ")
  }


  def show(frwBars :Seq[BarForwardRes]) : Unit = {
    println(" SUMMARY VISUAL : barsHist=["+barsHist.size+"] barsCurr=["+barsCurr.size+"] barsFound=["+barsFound.size+"]")

    for (bh <- barsHist.Data){
      print(bh+" ")
      for (bf <- barsFound.Data){
        if (bh == bf)
          print("   * ")
      }

      val idxInPair : Int = frwBars.map(x => x.endBar).flatten.indexOf(bh)
      if ( idxInPair > 0 )
        print("         out idxInPair="+idxInPair+"  BEGIN: "+frwBars(idxInPair).beginBar.bNumEnd+" rType="+frwBars(idxInPair).beType+" rHight="+frwBars(idxInPair).beNumRes+"   END: "+frwBars(idxInPair).endBar)

      println(" ")
    }
    println("   ")


  }

}



class histForwardAnalyzing(barsHist : BarsDS, barsFound: BarsDS, hValueHight : Int){

  def get_forward_bars : Seq[BarForwardRes] = {

    def getForwardSearchBar(startBar : Bar) : BarForwardRes ={


      // partition divide seq on 2 parts:
      // ._1 before Bar - startBar and ._2 after startBar
      // In second part make find element with conditions

      def findBar(cBar : Bar, compareBar : Bar) : Boolean ={
       if (
           (cBar.bHigh > startBar.bClose+hValueHight) /*u*/ ||
           (cBar.bLow  < startBar.bClose-hValueHight) /*d*/
          )
         true
       else
         false
      }

      new BarForwardRes(startBar, barsHist.Data.partition(_.bNumBegin < startBar.bNumEnd)._2.find(x => findBar(x,startBar)),hValueHight)
    }

    for(currBarFound <- barsFound.Data) yield getForwardSearchBar(currBarFound)
  }

}




object CurryingFuncs extends App {                 //simple
  val ticks : TicksDs = new scvReader("data/first.csv").readTicks
  println("-----------------------------------")
  println("ticks.size="+ticks.size+" ticks(Class): "+ticks.getClass.getName)
  println("-----------------------------------")
  /*
  //simple output of ticks
  for (tick <- ticks.Data) {
    println(tick.tNum+" "+tick.tVal+" "+tick.getClass.getName)
  }
  */

  val bars : BarsDS = new BarBuilder(ticks, 30).getBars

  println("-----------------------------------")
  println("bars.size="+bars.size)
  println("-----------------------------------")
  //simple output of ticks
  //for (thisBar <- bars.Data) println(thisBar)
  /*
  val avgB : Float = bars.Data.map(x=>(x.bHighBody)).sum.toFloat/bars.Data.size
  val avgS : Float = bars.Data.map(x=>(x.bHighShad)).sum.toFloat/bars.Data.size
  println("Avg Bars Body Hight =" +  avgB )
  println("Avg Bars Shad Hight =" +  avgS )
  println("S/B =" +  avgS/avgB )
  */

  val ticksCurr : TicksDs = new scvReader("data/current.csv").readTicks
  val barsCurr : BarsDS = new BarBuilder(ticksCurr, 30).getBars

  println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
  println("bars.size     = "+bars.size)
  println("barsCurr.size = "+barsCurr.size)
  println("~~ barsCurr Patter for search ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
  for (cb <- barsCurr.Data) println("          "+cb)
  println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")



  val barsHistSeacher : PatterSearcher = new PatterSearcher(bars,barsCurr)

  println("===================================")
  val searchHistRes : BarsDS = barsHistSeacher.patterSearchHistory
  println("Found in history (last bars in group):"+searchHistRes.size)
    for (resHistFound <- searchHistRes.Data) println(resHistFound)

  // -------------------------------------------------------------------------------------------
  // STEP:3 All history Bars visualisation with Current and comparison results.
  // -------------------------------------------------------------------------------------------
  val visualSearch = new VisualSearchResults(bars,barsCurr,searchHistRes)
  //visualSearch.show

  println("=== STEP 4 ================================")
  println(" ")
  println(" Forward search ")
  println(" ")

  val frwBars : Seq[BarForwardRes] = new histForwardAnalyzing(bars, searchHistRes, 20).get_forward_bars


  println("frwBars.size="+frwBars.size)
  println(" Last bar in founded pattern(hist)                        duration      EXIST bar")
    for (currResPair <- frwBars) println(currResPair.beginBar+"    >    "+currResPair.resTicksDuration+"  "+currResPair.endBar)



  println("=== STEP 5 ================================")
  println(" ")
  println(" Summary visialization ")
  println(" ")

  visualSearch.show(frwBars)

  println("=== STEP 6 ================================")
  println(" ")
  println(" Pattern founded in history need classification, f.e. total 560, 460-u ,100-d   Data can be added in real time with interval f.e. 1 min.")
  //we need use caches (for source ), queues,
  println(" ")


}
