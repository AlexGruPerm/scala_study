object TestCollectFunctions extends App{

  //is it possible go through tuple in loops
  def test_loop_by_tuple : Unit = {
    val t : Tuple5[Int, Int, Int ,Int, Int] = Tuple5(1,2,3,4,5)
    println(t)

    //for (ti <- t) println(ti)

  }

  def sumAll=(x:Int,y:Int) => {
    val res : Int = x+y
    println ("x="+x+" y="+y+" res="+res)
    res
  }


  def test_reduceLeft : Unit ={
    val s : Seq[Int] = Seq(1,2,3,4,5,6)
    println(s.reduceLeft(sumAll))
  }

  def checkTW = (x : Int) => {
   if (x<4) true
    else false
  }

  def test_parts : Unit = {
   val s : Seq[Int] = Seq(2,2,3,1,4,1,1,1,1,1,1,5,6,9,0)

    println(s.head)
    println(s.init)
    println(s.tail)
    println(s take 2)
    println(s take 2)
    println(s.drop(5))

    println("dw="+s.dropWhile(x => (x>1)))

    println("tw="+s.takeWhile(x => checkTW(x)))

    println("    ")
    println(s)

  }

  //test_loop_by_tuple
  //test_reduceLeft
   test_parts

}
