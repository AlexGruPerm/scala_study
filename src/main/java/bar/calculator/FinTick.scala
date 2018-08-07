package bar.calculator

case class FinTick(ts : Long, db_tsunx : Long, ask :Double, bid : Double){
  override def toString = ts+" "+db_tsunx+" "+ask+" "+bid

}
