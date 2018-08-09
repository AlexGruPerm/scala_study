package bar.calculator

class Bar (p_ticker_id : Int, p_bar_width_sec : Int, barTicks : Seq[TinyTick]) {

  def simpleRound4Double(valueD : Double) = {
    (valueD * 10000).round / 10000.toDouble
  }

  def simpleRound6Double(valueD : Double) = {
    (valueD * 1000000).round / 1000000.toDouble
  }

  val ticker_id       :Int = p_ticker_id
  val ddate           :Long = barTicks(0).ts
  val bar_width_sec   :Int= p_bar_width_sec
  val ts_begin        :Long = barTicks(0).ts
  val ts_end          :Long = barTicks.last.ts
  val o               :Double = simpleRound4Double((barTicks(0).ask + barTicks(0).bid)/2)
  val h               :Double = simpleRound4Double(barTicks.map(x => (x.ask+x.bid)/2).max)
  val l               :Double = simpleRound4Double(barTicks.map(x => (x.ask+x.bid)/2).min)
  val c               :Double = simpleRound4Double((barTicks.last.ask + barTicks.last.bid)/2)
  val h_body          :Double = simpleRound4Double(math.abs(c-o))
  val h_shad          :Double = simpleRound4Double(math.abs(h-l))
  val btype           :String =(o compare c).signum match {
    case -1 => "g" // bOpen < bClose
    case  0 => "n" // bOpen = bClose
    case  1 => "r" // bOpen > bClose
  }
  val ticks_cnt       :Int = if (barTicks.nonEmpty) barTicks.size else 0
  //standard deviation
  val disp            :Double =  if (ticks_cnt != 0)
                                  simpleRound6Double(
                                    Math.sqrt(
                                      barTicks.map(x => Math.pow((x.ask+x.bid)/2,2)).sum/ticks_cnt-
                                        Math.pow( barTicks.map(x => (x.ask+x.bid)/2).sum/ticks_cnt ,2)
                                    )
                                  )
                                 else 0
  val log_co          :Double = simpleRound4Double(Math.log(c)-Math.log(o))

  override def toString =
    "[ "+ts_begin+":"+ts_end+"] ohlc=["+o+","+h+","+l+","+c+"] "+btype+"   body,shad=["+h_body+","+h_shad+"]"

}
