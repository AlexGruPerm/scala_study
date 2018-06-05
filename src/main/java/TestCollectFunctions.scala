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

  //test_parts_simple
  test_divide_seq_on_bars

}
