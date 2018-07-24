

case class TTick(ts : Int)

object BarTaker extends App {
  println("BarTaker")
  val srcSeq = Seq(TTick(1),TTick(3),TTick(5),TTick(9),TTick(10),TTick(16),TTick(20),TTick(21),TTick(22),TTick(23),TTick(50),TTick(55))
  val wdh : Int = 10


  val fIdx = srcSeq.head.ts
  val lIdx = srcSeq.last.ts
  val barsSides = fIdx.to(lIdx).by(wdh)

  val seqBarSides = for ((bs,idx) <- barsSides.zipWithIndex) yield {
    (bs,idx.toInt)
  }

  //val seqBarSidesL = seqBarSides.last

  val seqBar2Sides = for(i <- 0 to seqBarSides.size-1) yield {
    if (i < seqBarSides.last._2)
      (seqBarSides(i)._1, seqBarSides(i+1)._1, seqBarSides(i)._2+1)
    else
      (seqBarSides(i)._1, seqBarSides(i)._1+wdh, seqBarSides(i)._2+1)
  }

  println(seqBar2Sides)
  println("          ")


  def getGroupThisElement(elm : Int)={
    seqBar2Sides.find(bs => (bs._1 <= elm && bs._2 > elm)).map(x => x._3)
  }

  val res = srcSeq.groupBy(elm => getGroupThisElement(elm.ts)).toSeq.sortBy(gr => gr._1)

  for (celm <- res){
    println(celm._1+" "+celm._2.map(st => st.ts))
  }


}