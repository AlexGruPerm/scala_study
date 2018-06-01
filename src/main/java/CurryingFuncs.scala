
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

/**
  *
  * @param barTicks : Seq[Tick] - Sequence of ticks that define this Bar.
  *
  * Instance of class represent one Bar
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
                                                        case  0 => "n"          // bOpen = bClose
                                                        case  1 => "r" // bOpen > bClose
                                                      }

  override def toString =
   "[ "+bNumBegin+":"+bNumEnd+"] ohlc=["+bOpen+","+bHigh+","+bLow+","+bClose+"] ( "+bType+" ) width,body,shad=["+bWidth+","+bHighBody+","+bHighShad+"]"

}

/** presents sequence of bars (instances of Bar class)
  *
  * @param Data
  *
  */
case class BarsDS(Data : Seq[Bar]){
  def size : Int = Data.size
}

/** Make sequence of Bar (instance BarsDS) from sequence of Tick (instance TicksDs)
  *
  * @param ticks        - source sequence of ticks (instances of class Tick)
  * @param ticksCntBar  - how many ticks in one Bar
  */
class BarBuilder(ticks : TicksDs, ticksCntBar : Int){

  def getBars : BarsDS = {
    // ok,but!!! val barsDS : BarsDS = new BarsDS(ticks.Data.sliding(ticksCntBar).toSeq.map(x => getBar(x)))

    // local recursive function that start form index i and end with index n, go through Seq,
    // and return parts 1 - n, n+1 - n+1+n ...
    def get_part(st: Seq[Tick], startIndex : Int, endIndex: Int) : Seq[Tick] = {
      if ( startIndex == endIndex ) {
        //return Seq(st.head)
        return Seq(st(startIndex))
      } else {
        //Seq(st.head) ++ get_part(st.tail,i+1,n)
        Seq(st(startIndex)) ++ get_part(st,startIndex+1, endIndex)
      }
    }

    val sb : Seq[Bar] = for(i <- 0 to ticks.size-1 by ticksCntBar if ((i + ticksCntBar - 1) <= (ticks.size-1)) ) yield {
                                //val beginIndex : Int = i
                                //val endIndex   : Int = beginIndex + ticksCntBar - 1
                                //val vb : Bar = new Bar(get_part(ticks.Data, beginIndex, endIndex))
                                // fdebug: val vb : Bar = new Bar((   Seq(new Tick(Tuple2(1,10)))  ))
                                //vb
                                new Bar(get_part(ticks.Data, i, i + ticksCntBar - 1))
                            //new Bar(get_part(ticks.Data,i,ticksCntBar))
                         }

    val barsDS : BarsDS = new BarsDS(sb)
      barsDS

  }

}


class PatterSearcher(barsHist : BarsDS, barsCurr :BarsDS){

  /**  In the history of bars (object bars) search current formation (patter) - object barsCurr
   *    and return all !-LAST-! bars of founded formations as an object of class BarsDS
    * */
  def patterSearchHistory : BarsDS = {

    def get_part(seqBars: Seq[Bar],startIndex : Int, n : Int) : Seq[Bar] ={
      if ( startIndex == n ) {
        return Seq(seqBars(startIndex))
      } else {
        Seq(seqBars(startIndex)) ++ get_part(seqBars,startIndex+1, n)
      }
    }

    //Compare 2 sequence of bars (Seq[Bar]) with same saze.

    def compHistPartCurr(histPart : BarsDS, barsCurr: BarsDS): Boolean = {
     true
    }

    val histFoundLastBars : Seq[Bar] = for(i <- 0 until barsHist.Data.size-barsCurr.size) yield {
      //println("get indexes: ("+i+","+(i+barsCurr.size-1).toInt+")")
      //val partHist : Seq[Bar] = get_part(barsHist.Data,i,(i+barsCurr.size-1))
      val partHistBars : BarsDS = new BarsDS(get_part(barsHist.Data,i,(i+barsCurr.size-1)).toSeq)
      // use here filter with custom function compHistPartCurr

      partHistBars.map(x => compHistPartCurr(x,barsCurr)
      /*
      if (compHistPartCurr(partHistBars,barsCurr)) {
         partHistBars.Data.last
      } else {
         partHistBars.Data.last
      }
      */

      // Seq(new Bar(Seq(new Tick(Tuple2(1,1)))))
    }

    new BarsDS(histFoundLastBars)
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
  for (thisBar <- bars.Data) println(thisBar)

  val avgB : Float = bars.Data.map(x=>(x.bHighBody)).sum.toFloat/bars.Data.size
  val avgS : Float = bars.Data.map(x=>(x.bHighShad)).sum.toFloat/bars.Data.size

  println("Avg Bars Body Hight =" +  avgB )
  println("Avg Bars Shad Hight =" +  avgS )
  println("S/B =" +  avgS/avgB )


  /*
  for (i <- 2 to 30){
    val bars : BarsDS = new BarBuilder(ticks, i).getBars
    val avgB : Float = bars.Data.map(x=>(x.bHighBody)).sum.toFloat/bars.Data.size
    val avgS : Float = bars.Data.map(x=>(x.bHighShad)).sum.toFloat/bars.Data.size
    println("i="+i+" S/B =" +  avgS/avgB )
  }
  */
  println("-----------------------------------")

  val ticksCurr : TicksDs = new scvReader("data/current.csv").readTicks
  val barsCurr : BarsDS = new BarBuilder(ticksCurr, 30).getBars

  for (cb <- barsCurr.Data) println(cb)

  val barsHistSeacher : PatterSearcher = new PatterSearcher(bars,barsCurr)

  println("===================================")

  val searchHistRes : BarsDS = barsHistSeacher.patterSearchHistory

  for (resHistFound <- searchHistRes.Data) println(resHistFound)

}
