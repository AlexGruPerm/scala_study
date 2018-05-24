import scala.io

object CollectStudy  extends App {

  val vSeq = Seq(2,3,1,4,2,3,3,5,5,7,7,0,0)

    println("vSeq          = "+vSeq)
    println("vSeq(0)       = "+vSeq(0))       //2
    println("vSeq.apply(1) = "+vSeq.apply(1)) //3

    println("is exists 4 index :"+vSeq.isDefinedAt(4)) //true
    println("is exists 5 index :"+vSeq.isDefinedAt(5)) //false

    println("seq size="  +vSeq.size)
    println("seq length="+vSeq.length)

    //vSeq.indices - has type Range
    println("vSeq.indices.toList="+vSeq.indices.toList)
    println("vSeq.indices.toArray="+vSeq.indices.toArray)
    println("vSeq.indices.toSet="+vSeq.indices.toSet)

    //Loop by Range
    for (x <- vSeq.indices) {
      println(x)
    }

    //Index Search:
    println("First position of element 1 in Seq ="+vSeq.indexOf(1)) // = 2 index begins from 0.
    println("Last position of element 2 in Seq ="+vSeq.lastIndexOf(2)) //=4

    println("First index of slice (1,4) = "+vSeq.indexOfSlice(Seq(1,4))) //=2

    println("vSeq.isEmpty="+vSeq.isEmpty)   //=false
    println("vSeq.nonEmpty="+vSeq.nonEmpty) //=true

    println("vSeq.indexWhere(x => x>3)="+vSeq.indexWhere(x => x>3)) //=3 value eq 4 is grater then 3

    println("vSeq.sliding(2)="+vSeq.sliding(2))
    for(x <- vSeq.sliding(2)){
      println(x)
    }

     println(vSeq.sliding(2).indexWhere{case Seq(x1,x2) => x1==x2})//=5 because List(3, 3)

     //indexes of first element of pairs where f and s equal.
     for(x <- vSeq.sliding(2).zipWithIndex.collect{ case(Seq(x1,x2),index) if (x1==x2) => index}.toList) println("res="+x)

     println("indexWhere="+vSeq.indexWhere(x => x > 3 && x < 5))//=3 because 4 has index 3

     println("vSeq.forall = "+vSeq.forall{elm => elm >= 0 && elm <= 100}) //check all elements between 0 and 100 = true

}
