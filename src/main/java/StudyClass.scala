import scala.io.Source
import java.net._
import scala.collection.immutable.IndexedSeq

  /**
  *
  *   Application's main class with method main(args)
  *
  **/

  trait ObectBase{

    def getName():String{}

    def getAttr():List[Int]{}

  }

  class ObjectSpec(attrSource : List[Int],objectName : String) extends ObectBase{
    private val attr = attrSource
    private val name = objectName
    override def getAttr():List[Int] = this.attr
    override def getName():String = this.objectName
  }

  class ObjectSpecAny(attrSource : List[Int],objectName : String) extends ObectBase{
    private val attr = attrSource
    private val name = objectName
    override def getAttr():List[Int] = this.attr
    override def getName():String = this.objectName
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
  val listObj1 = for (x <- 1 to 100)
                 yield new ObjectSpec(genRandomIntList(lengthList = 3), genRandomName(lengthList = 10))

  val listObj2 = for (x <- 1 to 100)
                 yield new ObjectSpecAny(genRandomIntList(lengthList = 3), genRandomName(lengthList = 10))

  val listObj : IndexedSeq[ObectBase] = listObj1++listObj2

  // Just output objects
  println("There are "+listObj.length+" object in the List. List has Class name - "+listObj.getClass.getName+" type- "+listObj.getClass.getTypeName)
  println()

  //output just 3 objects as example.
  for(i <- 1 to 3){
    println(listObj(i).getName()+"  "+listObj(i).getAttr())
  }
  /*
  for (oneObj <- listObj) {
    println(oneObj.getName()+"  "+oneObj.getAttr())
  }
  */

  // value for search in attr field of
  val searchInt : Int = 50

  //recursive function to search value in objects

  def searchVal(sVal: Int, searchFromIndex : Int, listObj : scala.collection.immutable.IndexedSeq[ObectBase]):String={
    if (searchFromIndex == listObj.length ) return "not found"
   //println("searchFromIndex="+searchFromIndex+" objectName="+listObj(searchFromIndex).getName())
   if (listObj(searchFromIndex).getAttr().contains(sVal)) {
     println("StepNUmber is ["+searchFromIndex+"] Result attr:"+listObj(searchFromIndex).getAttr())
     listObj(searchFromIndex).getName()
   }
    else searchVal(sVal, searchFromIndex+1, listObj)
  }

  println(" ")
  println("RESULE = "+searchVal(searchInt, 0, listObj))



  println("--------------------------------------------------------------------")
}