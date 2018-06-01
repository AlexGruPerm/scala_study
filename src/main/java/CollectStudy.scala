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

    println("vSeq.sliding(2)="+vSeq.sliding(3))
    for(x <- vSeq.sliding(3)){
      println(x)
    }

     println(vSeq.sliding(2).indexWhere{case Seq(x1,x2) => x1==x2})//=5 because List(3, 3)

     //indexes of first element of pairs where f and s equal.
     for(x <- vSeq.sliding(2).zipWithIndex.collect{ case(Seq(x1,x2),index) if (x1==x2) => index}.toList) println("res="+x)

     println("indexWhere="+vSeq.indexWhere(x => x > 3 && x < 5))//=3 because 4 has index 3

     println("vSeq.forall = "+vSeq.forall{elm => elm >= 0 && elm <= 100}) //check all elements between 0 and 100 = true

    /*
    *  SLICING
    *  index      : 0,1,2,3,4,5,6,7,8,....
    *  source Seq : 2,3,1,4,2,3,3,5,5,7,7,0,0
    *  (0,3)      : 2,3,1           -- without last index
    *  (3,4)      :       4,2,3,3   --
    *
    * */

     println("Slice 0-defined-1 = "+ vSeq.slice(3-3,3)) // 2, 3, 1 - from 0 to 3 - excluding 3
     println("Slice defined+3 = "+ vSeq.slice(3,3+4))   // 4, 2, 3 - from 3 to 7 - excluding 7

     //declare function as value
     val func0 = (inp : Int) => inp * 100
     val func1 = (intVal : Int) => 2*func0(intVal)
     println(" after .func0.func1 ")
     for (elm <- vSeq.map(x => func1(x))) print(elm+", ")

     println(" ")
     val fact:(Int=>Int) = (n) =>
     {
        n match {
                 case 0 => 1
                 case y => y*fact(y-1)
                }
     }

     println("fact(4) = "+fact(4))

     println("Nil == List() : "+Nil == List())
     println("Nil == Seq()  : "+Nil == Seq())

     println("-------------------")

     val s1 = "123x"
     val s2 = null //"123x"

     if (s1 == s2)     println("equal") else println("not equal")
     if (s1 eq s2)     println("equal") else println("not equal")
     if (s1 equals s2) println("equal") else println("not equal")

  /*
     val seqTup = Seq((1,1), (2,3), (3,5), (4,8), (5,7), (6,5), (7,3), (8,1) ,(9,8))
     println(seqTup)
*/
  /*
     // solution 1
     val elmMax = seqTup(seqTup.map(x => x._2).indexOf(seqTup.map(y => y._2).max))
     println(elmMax)

     //solution 2
     val seqVals = seqTup.map(x => x._2)
     val maxVal  = seqVals.max
     val idxMax  = seqVals.indexOf(maxVal)
     val elm2Max = seqTup(idxMax)
     println(elm2Max)

    //solution 3 - get all maximums
    val elmMax = seqTup.filter{_._2 == (seqTup.map(y => y._2).max)}
    println(elmMax)
*/

}
