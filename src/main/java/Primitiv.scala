
case class FinTick(ts : Int)

object Primitiv extends App {

 // val seqTicks = Seq(FinTick(1), FinTick(3), FinTick(5), FinTick(8), FinTick(10), FinTick(12), FinTick(14), FinTick(16), FinTick(18), FinTick(19), FinTick(27), FinTick(47) )
 // val seqTicks = Seq(FinTick(1), FinTick(3), FinTick(5), FinTick(12) )

  val seqTicks = Seq(1,3,5,7,8,10,12,14,16,18,19,27,47)

  /*
  val wdth = 10.toInt

  val seqBars = seqTicks.foldLeft(Seq.empty[Seq[Int]]){ (acc : Seq[Seq[Int]], curr : Int) =>

    if (acc.nonEmpty) {

      println("acc.size="+acc.size+" curr="+curr)

      if ((acc.last.last - acc.last.head) <= wdth) {
        val al : Seq[Int] = acc.last
        val res1 = acc.init :+ Seq(al :+ curr)
        println("R1="+res1)
        res1
      } else {
        val res2 =  acc :+ Seq(curr)
        println("R2="+res2)
        res2
      }

    } else
      Seq(Seq(curr))
  }



  println("                      ")
  println("                      ")
  println("seqBars.size="+seqBars)
  for(b <- seqBars) {
    println(b)
  }

  */

}
