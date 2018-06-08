

object SearchMaxs extends App {

  def getLength(s : Seq[Tuple2[Int,Int]]): Int = {
    s.length
  }

  /*
  * Return count of elements that less then one defined from right side:
  *
  * fs           - Full sequence
  * currSeqIndex - index of current element
  * deep         - deep of check for both side around current element
  *
  * */
  def get_LT_Count(fs : Seq[Tuple2[Int,Int]], currSeqIndex : Int, deep: Int): Int ={
    if ((currSeqIndex < deep) || (currSeqIndex > fs.length-deep-1) ) return 0

    val partSeqLeftPart : Seq[Tuple2[Int,Int]] = fs.slice(currSeqIndex-deep*2,currSeqIndex)
    val partSeqRigthPart : Seq[Tuple2[Int,Int]] = fs.slice(currSeqIndex+1,currSeqIndex+deep+1)
    val partSeq = partSeqLeftPart ++ partSeqRigthPart

    //println("partSeq.length="+partSeq.length+" currSeqIndex="+currSeqIndex+" vals="+partSeq+" fs(currSeqIndex)._2="+fs(currSeqIndex)._2+"  maxVal="+partSeq.map(x => x._2).max+" partSeq.length="+partSeq.length)


    //#2

    val currVal = fs(currSeqIndex)._2
    val cntLT : Int = partSeq.count(_._2 < currVal)

    return cntLT

    //#1
    //if (fs(currSeqIndex)._2 > partSeq.map(x => x._2).max) partSeq.length else 1
  }

  /*
  * Return sequence of Integer - Local maximums.
  * Tuple3:
  * 1 - X coord
  * 2 - Y coord
  * 3 - power of point. Count of point LT this from both sides.
  *
  * */
  def getLocalMaxs(srcSeq : Seq[Tuple2[Int,Int]],searchDeep : Int) : Seq[Tuple3[Int,Int,Int]] = {
    val procSeq : Seq[Tuple3[Int,Int,Int]] = for ((elm,idx) <- srcSeq.zipWithIndex) yield {
          val cntLT : Int = get_LT_Count(srcSeq,idx/*+1*/,searchDeep)
          (elm._1,elm._2,cntLT)
         }
   return procSeq//Seq((1,2,3))
  }

  /*
  *
  * Return filtered Seq.
  * Remove neighboring point.
  *
  * */
  def filter_result(resSeq : Seq[Tuple3[Int,Int,Int]]): Seq[Tuple3[Int,Int,Int]]  = {
    val resF : Seq[Tuple3[Int,Int,Int]] = resSeq.map(x => (scala.math.round(x._1.toFloat/100).toInt*100,x._2,x._3))
    val xSet : Set[Int] = resF.map(x => x._1).toSet
     //println("=================")
     //println(xSet)
     //println("=================")
    def fintGetY(setKey : Int) : Int ={
      return resF(resF.map(x => x._1).indexOf(setKey))._2
    }
    def fintGetCnt(setKey : Int) : Int ={
      return resF(resF.map(x => x._1).indexOf(setKey))._3
    }
    val totalRes :  Seq[Tuple3[Int,Int,Int]] = xSet.map(k => (k,fintGetY(k),fintGetCnt(k))).toSeq
    return totalRes//resF
  }


  //-------------------------------------------
  val buff = scala.io.Source.fromFile("data/test.csv")
  //val vCSV  : Seq[Tuple2[Int,Int]] = Seq((0,0),(1,10),(2,20),(3,30),(4,20),(5,10),(6,0))
  val vCSV  : Seq[Tuple2[Int,Int]] = buff.getLines.map{oneLine => (oneLine.split(";")(0).toInt-1, oneLine.split(";")(1).toInt)}.toSeq
  println("Source sequence length :" + getLength(vCSV)+ vCSV(0))
  //println(vCSV)
  println("--------------------------------------")
  // Final result - sequence of Tuples that are local maximums.
  val searchDeep : Int = 1000
  val seqMaxPoints = getLocalMaxs(vCSV,searchDeep)

  val res = seqMaxPoints.filter{_._3 >= 2996}
  println("Results gt 2 LENGTH = "+res.length)
  println("---------------------------------")
  for(resElm <- res) println(resElm)
  println("---------------------------------")
  val fltRes : Seq[Tuple3[Int,Int,Int]] = filter_result(res)
  println("Filtered results res = "+fltRes.length)
  println("---------------------------------")
  for(fresElm <- fltRes) println(fresElm)
  println("---------------------------------")

  buff.close

}
