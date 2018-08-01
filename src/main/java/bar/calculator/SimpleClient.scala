package bar.calculator

import com.datastax.driver.core.{Cluster, Row}

//EXAMPLES  https://github.com/magro/play2-scala-cassandra-sample/blob/master/app/models/Cassandra.scala
class SimpleClient(node: String) {

  private val cluster = Cluster.builder().addContactPoint(node).build()
  val session = cluster.connect()

  def loadData() = {
    session.execute(
      """INSERT INTO simplex.songs (id, title, album, artist, tags)
      VALUES (
          756716f7-2e54-4715-9f00-91dcbea6cf50,
          'La Petite Tonkinoise',
          'Bye Bye Blackbird',
          'Joséphine Baker',
          {'jazz', '2013'})
          ;""");
  }


  def querySchema1() = {
    val results = session.execute("select * from finance.currency;")
    val rowToCurrPair = (row: Row) => new CurrPair(row.getInt("id"),row.getString("name"))
    val rsList = results.all()
    val rsListSize = rsList.size()
    println("rsList.size="+rsList.size())
    for(i <- 0 to rsListSize-1){
      println(rowToCurrPair(rsList.get(i)).id +" "+rowToCurrPair(rsList.get(i)).name)
    }
  }

  def querySchema2() = {
    val results = session.execute("select * from finance.currency;")
    val rowToCurrPair = (row: Row) => new CurrPair(row.getInt("id"),row.getString("name"))
    val iterator = results.iterator()

    while(!results.isExhausted()) {
      results.fetchMoreResults()
      val row = iterator.next()
      if (row != null) {
        val cp = rowToCurrPair(row)
        println(cp.id +" "+cp.name)
      }
    }

  }


  def querySchema3() = {
    val results = session.execute("select * from finance.currency;")
    val rowToCurrPair = (row: Row) => new CurrPair(row.getInt("id"), row.getString("name"))
    val iter = results.iterator()

    while (iter.hasNext()) {
      if (results.getAvailableWithoutFetching() == 100 && !results.isFullyFetched())
        results.fetchMoreResults();
      val row = iter.next()
      if (row != null) {
        val cp = rowToCurrPair(row)
        println(cp.id + " " + cp.name)
      }
    }
  }

  // blahs.toIterator.map{ do something }.takeWhile(condition)
    def queryTicks() = {
      val results = session.execute("select ts,ask,bid from finance.tickdata where id_symbol=3 order by ts desc;")
      val rowToFinTick = (row: Row) => new FinTick(row.getTimestamp("ts"), row.getDouble("ask"), row.getDouble("bid"))
      val iter = results.iterator()
      var counter :Int = 0

      while (iter.hasNext()) {
        if (results.getAvailableWithoutFetching() == 100 && !results.isFullyFetched())
          results.fetchMoreResults();
        val row = iter.next()
        if (row != null) {
          val cp = rowToFinTick(row)
          counter = counter+1
        }
      }
      println("counter="+counter)
  }



  //==================================================
  /* 1)
  Iterator.iterate((blah, whatever)){ case (_,w) => (blah, some logic on w) }.
         takeWhile(condition on _._2).
         map(_._1)
  */
  //2)  .map{ do something }.takeWhile(condition)

  def getTicks() = {
    val results = session.execute("select ts,ask,bid from mts_src.ticks where ticker_id=4 and ddate='2018-07-13' order by ts desc;")
    val rowToFinTick = (row: Row) => new FinTick(row.getTimestamp("ts"), row.getDouble("ask"), row.getDouble("bid"))

    val rsList = results.all()
    for(i <- 0 to rsList.size()-1) yield rowToFinTick(rsList.get(i))
  }


  def getListTS()={
    val results = session.execute("""
                                    select max(ts) as ts,
                                           toUnixTimestamp(max(ts)) as tsunx
                                      from mts_src.ticks
                                     where ticker_id = 1 and
                                               ddate = '2018-07-16';
                                  """)

    val rsList = results.all()
    for(i <- 0 to rsList.size()-1)
      println(rsList.get(i).getTimestamp("ts")+"  "+rsList.get(i).getLong("tsunx"))
  }




  def close() {
    session.close
    cluster.close
  }


}
