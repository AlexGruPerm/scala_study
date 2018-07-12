import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.ResultSetFuture
import com.datastax.driver.core.Session
import scala.collection.JavaConverters._
import com.datastax.driver.core.{Metadata,Row}
import scala.language.implicitConversions


case class CurrPair(id : Int, name: String) {
  override def toString = id+" "+name
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
    val rowToCurrPair = (row: Row) => new CurrPair(row.getInt("id"),row.getString("name"))
    val iter = results.iterator()

    while (iter.hasNext()) {
      if (results.getAvailableWithoutFetching() == 100 && !results.isFullyFetched())
        results.fetchMoreResults();
      val row = iter.next()
      if (row != null) {
        val cp = rowToCurrPair(row)
        println(cp.id +" "+cp.name)
      }
    }




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
  client.querySchema3

  client.close
}
