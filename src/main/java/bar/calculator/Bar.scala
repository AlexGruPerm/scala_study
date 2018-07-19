package bar.calculator

class Bar (p_ticker_id : Int, p_bar_width_sec : Int, barTicks : Seq[FinTick]) {
  val ticker_id       :Int = p_ticker_id
  val ddate           :java.util.Date = barTicks(0).ts
  val bar_width_sec   :Int= p_bar_width_sec
  val ts_begin        :java.util.Date = barTicks(0).ts
  val ts_end          :java.util.Date = barTicks.last.ts
  val o               :Double = (barTicks(0).ask + barTicks(0).bid)/2
  val h               :Double = barTicks.map(x => (x.ask+x.bid)/2).max
  val l               :Double = barTicks.map(x => (x.ask+x.bid)/2).min
  val c               :Double = (barTicks.last.ask + barTicks.last.bid)/2
  val h_body          :Double = math.abs(c-o)
  val h_shad          :Double = math.abs(h-l)
  val btype           :String =(o compare c).signum match {
    case -1 => "g" // bOpen < bClose
    case  0 => "n" // bOpen = bClose
    case  1 => "r" // bOpen > bClose
  }
  val ts_end_unx      :Long   = barTicks.last.ts.getTime

  override def toString =
    "[ "+ts_begin+":"+ts_end+"] ohlc=["+o+","+h+","+l+","+c+"] "+btype+"   body,shad=["+h_body+","+h_shad+"]"

}
