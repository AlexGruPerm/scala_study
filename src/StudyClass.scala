import scala.io.Source
import scala.util.parsing.json._
import org.json4s._
import org.json4s.native.JsonMethods._

  /**
  *
  *   Application's main class with method main(args)
  *
  **/

object StartAppClass extends App{
  println("-- Begin ------------------------------------------")
   val s = Set(1,2,3,3,2,4,1,3,2,5,7,8,9)
    println(s.getClass.getName+"["+s.getClass.getTypeName+"]")
    println(s)
    println(" ")

    try {
      val vv = for (x <- s if (x < 2 || x > 8))
        yield x
      if (vv.size > 0) {
        for (x <- vv) {
          val resx = x match {
                            case 1 => println("small")
                            case 9 => println("big")
                            case _ => println("n/n")
                          }
          println(resx)
        }
      } else {
        throw new RuntimeException("vv is empty")
      }
    } catch {
      case ex: RuntimeException => println(ex.getMessage)
    } finally {
      println("Finally step")
    }

    /*

    val servUrl ="http://87.245.154.49/trading/service/new/armIndicators"
    val html = Source.fromURL(servUrl)
    val htmlString = html.mkString
    println(htmlString)
    */

    //https://github.com/json4s/json4s



  println("-- End ------------------------------------------")
}