object BarsPatternSearch extends App {
  val buff = io.Source.fromFile("data/test.csv")
  val vCSV :Seq[scala.Tuple2[Int,Int]] = buff.getLines.map{oneLine => (oneLine.split(";")(0).toInt-1,oneLine.split(";")(1).toInt)}.toSeq
  println("vCSV.Length = "+vCSV.size)
  /*
  for (i <- 0 to 5){
    println(vCSV(i)._1+" "+vCSV(i)._2)
  }
  */

  //index, open, close, hight
  val vBars : Seq[Tuple5[Int,Int,Int,String,Int]] = for(i <- 0 to vCSV.length-2) yield
             (vCSV(i)._1, vCSV(i)._2, vCSV(i+1)._2, if ((vCSV(i+1)._2-vCSV(i)._2)>=0) "g" else "r",vCSV(i+1)._2-vCSV(i)._2)

   println("---- Output vBars --------------------------")
   println("Index  Open  Close  Type  Size  ")
   for (i <- 0 to 4){
    println(vBars(i)._1+"      "+vBars(i)._2+" "+vBars(i)._3+" "+vBars(i)._4+" "+vBars(i)._5)
   }

   //посчитать частоту разных паттернов составом n=3 бара, по цветам.
   println("")
   println("################################################")
   println("")

   //Divide source sequence of tuples (Index  Open  Close  Type  Size) on sequences of sequences with Type elements
   // from source sequence. Set divider deep. n =3 Ex:
   //  g r r g r g g g
   // ((g,r,r) (r,r,g) (r,g,r) (g,r,g) (r,g,g) (g,g,g)
   // and calculate frequence of each group.
   val slidingDeep : Int = 3
   val seqSeqTypes : Seq[Seq[Tuple5[Int,Int,Int,String,Int]]] = vBars.sliding(slidingDeep).toSeq

   println("Source sequence divided on "+seqSeqTypes.size+" parts with length of each: "+seqSeqTypes(0).size)
   println("")
   println("Example: ")
   println(seqSeqTypes(0))
   println(seqSeqTypes(1))
   println(" ")

  // Input:   Vector((0,57840,57840,g,0), (1,57840,57834,r,-6), (2,57834,57831,r,-3))
  // Output:  (g,r,r)
  // For groupBy function
  def getBarTypesAsTupleN(barsN : Seq[Tuple5[Int,Int,Int,String,Int]]) : Seq[String] = {
    val res : Seq[String] = barsN.collect({case (i,o,c,t,h) => t})
    return res
  }
                                    // groupBy take a function that return group by WHAT
  val mapPatternCount = seqSeqTypes.groupBy(x => (getBarTypesAsTupleN(x)))
                                   .mapValues(_.size).toSeq
                                   .sortWith(_._2 > _._2)

  for (mapPC <- mapPatternCount){
    println(mapPC._2+"  "+mapPC._1)
  }

  println("")
  println(" ------ Second part----------------- ")
  println("")

  /*
  //He we calculate distribution of bars hight.
  val vBarsCnt : Int = vBars.size
  val vBarH = vBars.groupBy(x => x._5)
    .mapValues(_.size*100/vBarsCnt).toSeq
    .sortWith(_._1 > _._1)
  for (bh <- vBarH){
    println(bh._2+"  "+bh._1)
  }
  // RESULT:  prcnt,hight
  0  4
  3  3
  8  2
  13  1
  45  0
  12  -1
  8  -2
  3  -3
  1  -4
  0  -5
  */

  //we are going research only patterns where each bar has hight no more than 5 points by absolute value.
  //1. go through seq seqSeqTypes that contains slidingDeep bars.
  //    Like : Vector((0,57840,57840,g,0), (1,57840,57834,r,-6), (2,57834,57831,r,-3))
  //  if each bar in has enough hight then go through source vBars and analyze width (prof/loss)
  //  we need mark each pattern with u/d - up/down or n- not known, which event is rising earlier.
  // u d n

  val lookDeep : Int = 1000;
  val widthPrice : Int = 20;
  val limitBarHight : Int = 5;

  def checkAllBarHight(patt : Seq[Tuple5[Int,Int,Int,String,Int]]): Boolean = {
    //Input Seq of Tuples, return true if each Tuple element math.abs(#5) less or equal than limitBarHight
    if (patt.filter(oneBar => math.abs(oneBar._5) <= limitBarHight).size == patt.size) true else false
  }

  println("Total bars pattern count = "+seqSeqTypes.size)
  // filter remove element if function return False.
  println("With filter        count = "+seqSeqTypes.filter(x => checkAllBarHight(x)).size)

  /*
   Take pattern as input.
   Use "lookDeep" as deep - bar count to look forward and "widthPrice" to check.
   Use "vBars" to search
   Example input:
   Vector((8,57818,57820,g,2),
          (9,57820,57820,g,0),
          (10,57820,57823,g,3))
  */
  def testPattern(patt : Seq[Tuple5[Int,Int,Int,String,Int]]) : scala.Tuple2[String,Int] = {
    // "u" or "d" or "n"
    // .lastOption
    // https://stackoverflow.com/questions/5104087/choosing-the-last-element-of-a-list
    val startIndex : Int = patt.last._1+1 // Index, get last add 1 to exclude checking with current bar

    //println("    last="+ patt.last)

    val pattPrice : Int = patt.last._3  // Close price - last

    //println("    startIndex="+startIndex)
    //println("    pattPrice="+pattPrice)

    var searchRes : String ="n"
    var i : Int = startIndex;

    do {
        //println("(i="+i+")   MAX="+Seq(vBars(i)._2, vBars(i)._3).max +"   MIN="+Seq(vBars(i)._2, vBars(i)._3).min)
        if (Seq(vBars(i)._2, vBars(i)._3).max > (pattPrice + widthPrice)) {
          searchRes = "u"
        } else if(Seq(vBars(i)._2, vBars(i)._3).min < (pattPrice - widthPrice)){
          searchRes = "d"
        }
      i=i+1
    } while (i<=startIndex+lookDeep && searchRes=="n" )
    return (searchRes,i)
  }

  /*
do {
   statement(s);
}
while( condition );


  (n compare 10).signum match {
    case -1 => "less than ten"
    case  0 => "ten"
    case  1 => "greater than ten"
}
  */

/*
  //loop only through appropriate patterns
  for (barPatt <- seqSeqTypes/*.take(20)*/ if checkAllBarHight(barPatt)) {
    //println("--------------------------")
      val sres = testPattern(barPatt)
       println(barPatt + "   " + sres._1 +" "+sres._2 + "   :"+vBars(sres._2))

  }
*/






  buff.close
}
