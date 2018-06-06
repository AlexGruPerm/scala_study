object TestCollectFunctions extends App{

  def test_parts_simple : Unit ={
    val v : Seq[Int] = for(i <- 1 to 10) yield i
    println(v)

    def get_rest(acc: Int, s : Seq[Int], limitV : Int) : Seq[Int] = {
     if (acc+s.head < limitV) get_rest(acc+s.head,s.tail,limitV)
     else s.tail
    }

    val r : Seq[Int] = get_rest(0,v,10)

    println(r)
  }

  def test_divide_seq_on_bars : Unit = {
    val src : Seq[Int] = for(i <- 1 to 95) yield i
    println("src="+src)
    println(" ")
    val barSize : Int = 10
    val bars : Seq[Seq[Int]] = src.sliding(barSize,barSize).filter(x => (x.size==barSize)).toSeq
    println("bars.size="+bars.size)
    bars.foreach(println)

  }

  def test_zip : Unit = {
   val s1 : Seq[String] = Seq("a","b","c")
   val s2 : Seq[Int] = Seq(3,4,5)

    for (x <- s1.zip(s2)) println(x._1+"  "+x._2)

  }

  def test_dw : Unit ={
    val s : Seq[Int] = Seq(1,2,3,4,5,6,7,8,9)
    println(s.partition(x => (x<5))._2)
    println(s.dropWhile(x => (x<5)))
    println(s.span(x => (x<5))._2)
  }

  //test_parts_simple
  //test_divide_seq_on_bars
  //test_zip

  test_dw

}
