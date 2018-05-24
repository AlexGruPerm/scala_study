import scala.math

object CSVAnalyze extends App {
  val buff = io.Source.fromFile("data/test.csv")
  val vCSV = buff.getLines.map{oneLine => (oneLine.split(";")(0).toInt,oneLine.split(";")(1).toInt)}.toSeq
  //vCSV is a Sequence of scala.Tuple2 objects.

  //strong of maximum, to exclude waste/weak
  println("-------------------------")
  for(i <- 0 to 5) println(vCSV(i)._1 + " " + vCSV(i)._2)
  println("-------------------------")

  //search local maximums as Sequences of source Sequence ( Seq(ind,val), Seq(ind,val) )
  val devFactor : Int = 5      //external parameter - how many parts in source seq
  val partPieceCount : Int = 5 //external parameter - how many pieces in local parts, for bounds excluding
  val seqLen : Int = vCSV.length
  val partLen : Int = scala.math.round(seqLen.toFloat/devFactor)
  println("seqLen="+seqLen+" devFactor = "+ devFactor+ " partLen = "+ partLen)
  println(" ")

  //loop by all tuples excluding elements on borders,
  //partLen from left and from right
  val interLeft  :Int = 1 + partLen
  val interRight :Int = vCSV.length - partLen

  println("Internal bounds : "+interLeft+" - "+interRight)

   val res : IndexedSeq[scala.Tuple2[Int,Int]] = for (i :Int <- interLeft to interRight) yield {
     val partSeq = vCSV.slice(i-partLen,i+partLen +1)
     val lowerIdx : Int = i - partLen
     val upperIdx : Int = i + partLen + 1
     //#println("i="+i+" partSeq.length="+partSeq.length+" from="+lowerIdx+" to="+upperIdx)

     // search in one local part.
     val seqVals = partSeq.map(x => x._2)
     val maxVal  = seqVals.max
     val idxMax  = seqVals.indexOf(maxVal)
     val idxMax_Ext = idxMax + lowerIdx
     //#println(" >>> "+idxMax)
     val koeff : Float = 0.99.toFloat

     if (idxMax_Ext >= (lowerIdx + koeff*partLen)  && idxMax_Ext <= (upperIdx - koeff*partLen) ) {
      // println(" !!!!! >>>>>>>>   i="+i+"   "+vCSV(i))
       vCSV(i)
     } else {
       scala.Tuple2(0,0)
     }

   }

  //check Type of each element
  val resPointsSeq : Seq[scala.Tuple2[Int,Int]] = for(elem <- res; if elem._1 != 0) yield (elem._1,elem._2)

  println(" SUMMARY, res.length="+resPointsSeq.length)

  for(x <- resPointsSeq) {
      println(x)
  }


  /*
  for (i :Int <- interLeft to interRight){
    //5649 elements in one part.
    val partSeq = vCSV.slice(i-partLen,i+partLen +1)
    val lowerIdx : Int = i - partLen
    val upperIdx : Int = i + partLen + 1
    //#println("i="+i+" partSeq.length="+partSeq.length+" from="+lowerIdx+" to="+upperIdx)

    // search in one local part.
    val seqVals = partSeq.map(x => x._2)
    val maxVal  = seqVals.max
    val idxMax  = seqVals.indexOf(maxVal)
    val idxMax_Ext = idxMax + lowerIdx
    //#println(" >>> "+idxMax)

    if (idxMax_Ext >= (lowerIdx + 0.6*partLen)  && idxMax_Ext <= (upperIdx - 0.6*partLen) ) {
      println(" !!!!! >>>>>>>>   i="+i+"   "+vCSV(i))
    }
  }
*/




  //println(vCSV(0).getClass.getName+" "+vCSV(0).getClass.getTypeName)

  //old  val vCSV = buff.getLines.map{oneLine => oneLine.split(";")(1)}.toSeq

  /*
  //output first 5 elements.
  for(i <- 0 to 5) println(vCSV(i))



  val seqParts = vCSV.sliding(partLen).toSeq //Seq of Seq with length partLen.

  //search maximum in each parts, excluding areas near left bound and right bound,
  // full length divided into partPieceCount parts.

  println("count of parts: "+seqParts.length)

  val partLeftInternal  = 0 + (partLen/partPieceCount)*2
  val partRigthInternal = partLen - (partLen/partPieceCount)*2

  for (i <- 0 to seqParts.length-1){

  }
*/

  /*
  for (i <- 0 to seqParts.length-1){
    val maxElementPart = (seqParts(i)_1).max
    val idxMaxElementInPart = seqParts(i).indexOf(maxElementPart)
      if (idxMaxElementInPart >= partLeftInternal && idxMaxElementInPart <= partRigthInternal) {
       println("OK: "+i)
      }

  }
  */






  buff.close
}
