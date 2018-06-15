import java.io.FileReader
import java.io.FileNotFoundException
import java.io.IOException

object Primitiv extends App {

/*
  def op1 = {
    println("op1")
    true
  }

  def op2 = {
    println("op2")
    true
  }

  if (op1 || op2) println("[TRUE]")
  else
    println("[ELSE]")
*/

  //1) With the “file <- filesHere” syntax, which is called a generator,
  for (x <- Seq(1,2,3))
    println(x)

  //2) 1 to 5 - Range
  for(i <- 1 until 6)
    println("i="+i)

  //3) for with filter
  for(j <- 1 until 10 if j<=5)
    println("j="+j)

  //4) for with filters
  for(t <- 1 until 10 if t >=3 if t<=7)
    println("t="+t)

  //5) Nested iteration
  val ds1 : Seq[Int] = Seq(1,2,3)
  val ds2: Seq[String] = Seq("x","y","z")

   for (num <- ds1; st <- ds2)
     println(num+" "+st)

  // При использовании фигурных скобок, ; можно опустить !?

  //curly braces instead of parentheses
  //6) Mid-stream variable bindings
  for {num <- ds1
       st <- ds2
        comb = num+st
      } println("comb="+comb)

  //Producing a new collection
  val newCol = for {xn <- ds1} yield (xn,xn*2)
  println(newCol)

  //try catch - catch обязательно должен быть в скобках
  //try and finally do not require parentheses if they contain only one expression.
  val x:Int=4
  val y:Int=1

  try {
    if (x/y==2)
      println("Ok")
    else
      throw new RuntimeException("x/y must be equal 2")
  } catch {
     case ex : RuntimeException => println("RuntimeException="+ex.getMessage)
     case ex : IOException => println("IOException")
  }

  def getEx = throw new RuntimeException("Just Ex for test")

  try
   getEx
  catch {
    case ex : RuntimeException => println("RuntimeException="+ex.getMessage)
    case ex : IOException => println("IOException")
  } finally
    println("Finally block of code")

}
