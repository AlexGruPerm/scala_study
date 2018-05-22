import scala.io.Source
import scala.util.parsing.json._
import java.net._

  /**
  *
  *   Application's main class with method main(args)
  *
  **/

  class ObjectSpec(attrSource : List[Int], objectName : String){
    require (attrSource.length > 0)
    private val attr = attrSource
    private val name = objectName
    def getAttr():List[Int] = this.attr
    def getName():String = this.objectName
  }



object StartAppClass extends App {
  println("-- Begin --------------------------------------------------------------")
  /*
    There is list of objects of defined class.
    Objects has a field with name attr that has List type and contains from Int numbers.
    We need create this object and write recursive function in function style to iterate through source List and
    find object with X in field attr.
   */

  // return List with length - lengthList, populated with random Int.
  def genRandomIntList(lengthList: Int): List[Int] = {
    List.fill(lengthList)(util.Random.nextInt(100))
  }

  // return random string with length - lengthList
  def genRandomName(lengthList: Int):String = {
    val r = new scala.util.Random
    val x :String = r.alphanumeric.take(lengthList).mkString
    return x
  }

  //  List of objects.
  val listObj = for (x <- 1 to 1000) yield new ObjectSpec(genRandomIntList(lengthList = 3), genRandomName(lengthList = 10))

  // Just output objects
  println("There are "+listObj.length+" object in the List. List has Class name - "+listObj.getClass.getName+" type- "+listObj.getClass.getTypeName)
  println()

  //output just 3 objects as example.
  for(i <- 1 to 3){
    println(listObj.apply(i).getName()+"  "+listObj.apply(i).getAttr())
  }
  /*
  for (oneObj <- listObj) {
    println(oneObj.getName()+"  "+oneObj.getAttr())
  }
  */

  // value for search in attr field of
  val searchInt : Int = 99

  //recursive function to search value in objects

  def searchVal(sVal: Int, searchFromIndex : Int, listObj : scala.collection.immutable.IndexedSeq[ObjectSpec]):String={
    if (searchFromIndex == listObj.length ) return "not found"
   //println("searchFromIndex="+searchFromIndex+" objectName="+listObj.apply(searchFromIndex).getName())
   if (listObj.apply(searchFromIndex).getAttr().contains(sVal)) {
     println("StepNUmber is ["+searchFromIndex+"] Result attr:"+listObj.apply(searchFromIndex).getAttr())
     listObj.apply(searchFromIndex).getName()
   }
    else searchVal(sVal, searchFromIndex+1, listObj)
  }

  println(" ")
  println("RESULE = "+searchVal(searchInt, 0, listObj))



  println("--------------------------------------------------------------------")
}