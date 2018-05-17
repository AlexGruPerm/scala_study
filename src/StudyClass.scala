

//When a singleton object
//shares the same name with a class, it is called that class’s companion object
object StudyClass {
  var varSingltineObject = 100

}

//A singleton object that does not share the same name with a companion
//class is called a standalone object.

//The class is called the companion class of the singleton object.
class StudyClass {
  /*
   Inside a class definition, you place fields and methods, which are collectively
   called members.
  */
  // Fields are also known as instance variables
  var className = "none"
  private var classValue = 0

  def add(b: Int): Unit = {
    this.classValue = b + StudyClass.varSingltineObject
  }

  def getClassValue: Int = this.classValue


}





object StartAppClass extends App{
  println("Begin")
  val sc1 = new StudyClass
  val sc2 = new StudyClass

  println(sc1.className)
  sc1.className="manually set new Name"
  println(sc1.className)

  sc1.add(10)
  println("Class Vlaue is: "+sc1.getClassValue)

  println("Like static field: "+StudyClass.varSingltineObject)
  StudyClass.varSingltineObject = 200
  println("set new value - Like static field: "+StudyClass.varSingltineObject)


  println("End")
}