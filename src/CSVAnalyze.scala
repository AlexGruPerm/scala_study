import scala.math

object CSVAnalyze extends App {

  val buff = io.Source.fromFile("data/test.csv")
  val vCSV = buff.getLines.map{oneLine => oneLine.split(";")(1)}.toSeq

  println("First: ["+vCSV(0)+"] size: ["+vCSV.size+"]")
  println("------------------------------------------")

  //output first 5 elements.
  for(i <- 0 to 5){
    println(vCSV(i))
  }
  println("------------------------------------------")

  //search local maximums as Seq of Seq( Seq(ind,val), Seq(ind,val) )
  val seqLen : Int = vCSV.length
  val devFactor : Int = scala.math.round(seqLen.toFloat/3000)
  println("devFactor = "+devFactor)



  buff.close
}
