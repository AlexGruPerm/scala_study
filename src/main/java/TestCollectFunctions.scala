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
  //test_dw

  def test_recursive_search : Unit ={
    //for(i <- 1 to 10) println("("+i+","+math.round(math.random()*10)+"),")
    //for(i <- 1 to 10) println("("+i+","+i+"),")

    var hist :Seq[Tuple2[Int,Int]] = Seq(
      (1,1),
      (2,2),
      (3,4),
      (4,6),
      (5,5),
      (6,4),
      (7,5)
      /*,
      (8,11),
      (9,8),
      (10,5)
    */
    )

    val hValueHight : Int = 4
    var curr :Seq[Tuple2[Int,Int]] = Seq((1,1),(3,4))

    // (1,1) -> begin with (2,2) result (4,6) because 6 > 1+4 =5
    // (3,4) -> begin with resule (8,10) because 10 > 4+4=8

    def rec_search(seqToSearch : Seq[Tuple2[Int,Int]], seqWhatSearchFirstElm : Seq[Tuple2[Int,Int]]) : Seq[Tuple2[Tuple2[Int,Int],Tuple2[Int,Int]]] = {
      if (seqWhatSearchFirstElm.size==1) {
        //last iteration
        Seq(
            ((seqWhatSearchFirstElm.head), seqToSearch.find(x => (x._2 > seqWhatSearchFirstElm.head._2+hValueHight || x._2 < seqWhatSearchFirstElm.head._2-hValueHight)).getOrElse(Tuple2(0,0)))
           )
      } else {
        Seq(
            ((seqWhatSearchFirstElm.head), seqToSearch.find(x => (x._2 > seqWhatSearchFirstElm.head._2+hValueHight || x._2 < seqWhatSearchFirstElm.head._2-hValueHight)).getOrElse(Tuple2(0,0)))
           ) ++
            rec_search(seqToSearch.span(x => (x._1 < seqWhatSearchFirstElm.head._1))._2, seqWhatSearchFirstElm.tail)
      }
    }

    val res : Seq[Tuple2[Tuple2[Int,Int],Tuple2[Int,Int]]] = rec_search(hist,curr)

    for(pair <- res) {
      println(pair._1+" - "+pair._2)
    }

  }

  test_recursive_search

}
