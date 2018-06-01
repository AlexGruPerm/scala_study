object HOFuncs extends App {

  def y(x : Double) : Double = {
    x*x+1
  }

  def numint(f: Double => Double,x : Double, xu: Double, dx :Double):Double = {
    if (x >= xu) 0
    else dx*f(x+dx/2) + numint(f,x+dx,xu,dx)
  }

  println(numint(y,x=0,xu=2,dx=0.01))

}


