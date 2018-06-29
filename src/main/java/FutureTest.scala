import scala.concurrent.{Future, Await}
import scala.util.Try
import scala.concurrent.duration._
import scala.util.{Success, Failure, Try}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @param engVolume in range 1.6 - 5.9
  * @param initGas
  */
class Car(name: String,engVolume : Double,initGas : Double){
  val speed = engVolume * 37.5
  val gasPerHour = engVolume * 5

  def driveCar(currGas : Double = initGas, dist : Double = 0 , hours : Int = 0) : Double = {
   if (currGas <= 0)
     dist
   else {
     Thread.sleep(1000)
     println("Car ["+name+"] drive "+dist+" km currGas="+currGas+" hours="+hours)
     dist + driveCar(currGas-gasPerHour, dist+speed, hours + 1)
   }
  }

  def beginDrive = Future {
    println("begin drive car=["+this.name+"]")
    this.driveCar()
    println("stop car = ["+this.name+"]")
  }

  def getFuturAsFunc = () => beginDrive

}


object FutureTest extends App {

  val cars : Seq[Car] = Seq(new Car("vaz",1.6,120),
                            new Car("nissan",2.0,150),
                            new Car("bmw",3.5,130))

  println(">>>")
  val listFuturesForStart = for(c <- cars) yield c.getFuturAsFunc

  //run it
  for {
   c <- listFuturesForStart
  } yield c()

  Thread.sleep(20000)
  println("<<<")
}
