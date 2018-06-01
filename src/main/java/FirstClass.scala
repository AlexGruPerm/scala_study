import scala.collection.mutable


object pc{
  def print_prv():Unit={
    println("this is a companion object")
  }
}

class pc{

  private val prvV = 123

  def test(a :Int, b :Int):Int ={
    a + b
  }
}



object FirstClass extends App{

  def printClassName(objName: String, obj : Any): Unit = {
    println(objName+":   "+obj.getClass.getName);
  }

  val p = new pc()
  println(p.test(30,4))

  println("===============================")

  var arr = Array("zero", "one", "two") // Array - Scala array is a mutable sequence of objects that all
                                        // share the same type.

  printClassName(objName="Array",obj=arr)

  val lst =List(1,2,3) // List - immutable sequence of objects that share the same type
  //println("lst class :"+lst.getClass.getName)
  printClassName(objName="List",obj=lst)

  val tpl = (1,"two",3.0) // Tuple - tuples are immutable, but unlike lists, tuples can contain
                          // different types of elements.
  //println("tpl class :"+tpl.getClass.getName+" "+tpl.getClass.getTypeName)
  printClassName(objName="Tuple",obj=tpl)

  val st = Set(1,2,3) //Set - immutable set
  //println("st class :"+st.getClass.getName)
  printClassName(objName="Set",obj=st)

  var stm = mutable.Set(1,2,3,4)
  //println("stm class :"+stm.getClass.getName)
  printClassName(objName="Set mutable",obj=stm)

  val mp = mutable.Map[Int, String]()
  mp += (1 -> "Go to island.")
  mp += (2 -> "Find big X on ground.")
  mp += (3 -> "Dig.")

  //println("mp class :"+mp.getClass.getName)
  printClassName(objName="Map",obj=mp)

  //-------------------------
  //var objTest = 1->"Some text"
  //println(objTest)
  //println(objTest.getClass.getName+" "+objTest.getClass.getTypeName )

  /*
  def main(args: Array[String]) = {
    println("FirstClass main method")
  }
  */

}

