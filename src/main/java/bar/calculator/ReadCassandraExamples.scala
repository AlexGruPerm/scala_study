package bar.calculator

object ReadCassandraExamples extends App {
  println("hello ")
  val client = new SimpleClient("127.0.0.1")

  //client.createSchema
  //client.loadData

  //client.querySchema1
  //client.querySchema2
  //client.querySchema3
  /*
  val t1 = System.currentTimeMillis
  client.queryTicks
  val t2 = System.currentTimeMillis
  println((t2 - t1) + " msecs")
  */

  /*
  val t1 = System.currentTimeMillis
  val ticksData = client.getTicks()
  val t2 = System.currentTimeMillis
  println("ticksData.size="+ticksData.size+" duration = "+(t2 - t1)+ " msecs")
  val sumAsk = ticksData.map(elm => elm.ask).sum
  println("sumAsk="+sumAsk)
*/


  //!!!!!!!!!!!!!
  val barCalc = new BarCalculator(client.session)
  barCalc.calc()

  //val t1 = System.currentTimeMillis
  //client.getListTS()
  //val t2 = System.currentTimeMillis


  //for ((elm,idx)  <- ticksData.zipWithIndex if idx<10) println(elm)

  /*
   val procSeq : Seq[Tuple3[Int,Int,Int]] = for ((elm,idx) <- srcSeq.zipWithIndex) yield {
          val cntLT : Int = get_LT_Count(srcSeq,idx/*+1*/,searchDeep)
          (elm._1,elm._2,cntLT)
         }
   */

  client.close
}
