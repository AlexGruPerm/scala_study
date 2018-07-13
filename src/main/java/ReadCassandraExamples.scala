import java.sql.ResultSet

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.ResultSetFuture
import com.datastax.driver.core.Session

import scala.collection.JavaConverters._
import com.datastax.driver.core.{Metadata, Row}
import org.apache.spark.sql.execution.streaming.FileStreamSource.Timestamp
import java.sql.Timestamp


import scala.language.implicitConversions
import scala.concurrent.{ ExecutionContext, Future}


case class CurrPair(id : Int, name: String) {
  override def toString = id+" "+name
}

case class FinTick(ts : java.util.Date, ask :Double, bid : Double){
  override def toString = ts+" "+ts.getTime+" "+ask+" "+bid

}

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






  def close() {
    session.close
    cluster.close
  }


}

object ReadCassandraExamples extends App {
  println("hello ")
  val client = new SimpleClient("127.0.0.1")

  //client.createSchema
  //client.loadData

  //client.querySchema1
  //client.querySchema2
  //client.querySchema3
  /*
  val t1 = System.currentTimeMillis
  client.queryTicks
  val t2 = System.currentTimeMillis
  println((t2 - t1) + " msecs")
  */

  val t1 = System.currentTimeMillis
  val ticksData = client.getTicks()
  val t2 = System.currentTimeMillis

  println("ticksData.size="+ticksData.size+" duration = "+(t2 - t1)+ " msecs")

  val sumAsk = ticksData.map(elm => elm.ask).sum
  println("sumAsk="+sumAsk)

  //sample


  for ((elm,idx) <- ticksData.zipWithIndex if idx<10) println(elm)


  /*
   val procSeq : Seq[Tuple3[Int,Int,Int]] = for ((elm,idx) <- srcSeq.zipWithIndex) yield {
          val cntLT : Int = get_LT_Count(srcSeq,idx/*+1*/,searchDeep)
          (elm._1,elm._2,cntLT)
         }
   */

  client.close
}
