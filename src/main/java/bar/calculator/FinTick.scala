package bar.calculator

case class FinTick(ts : java.util.Date, ask :Double, bid : Double){
  override def toString = ts+" "+ts.getTime+" "+ask+" "+bid

}
