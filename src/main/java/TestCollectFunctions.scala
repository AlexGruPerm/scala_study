object TestCollectFunctions extends App{

                  //idx=0  1     4
  val q : Seq[Int] = Seq(1,2,3,4,5,6,7,8,9,1,2,3,4,5,6)

  val idx: Int = q.indexOf(5)

  val s : Seq[Int] = q.takeRight(idx) // takeRight получает с конца n элементов.

  println("q="+q)
  println("idx="+idx)
  println("s= "+s)

}
