import bar.calculator.CurrPair
import com.datastax.driver.core._
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.implicitConversions
import scala.collection.JavaConverters._
import scala.util.{Failure, Success}


case class CurrPair(id : Int, name: String)


object CassandraWriteRead extends App {
  println("CassandraWriteRead examples")
  /*
  implicit class CqlStrings(val context: StringContext) extends AnyVal {
    def cql(args: Any*)(implicit session: Session): ListenableFuture[PreparedStatement] = {
      val statement = new SimpleStatement(context.raw(args: _*))
      session.prepareAsync(statement)
    }
  }
  */

  implicit class CqlStrings(val context: StringContext) extends AnyVal {
    def cql(args: Any*)(implicit session: Session): Future[PreparedStatement] = {
      val statement = new SimpleStatement(context.raw(args: _*))
      session.prepareAsync(statement)
    }
  }


  implicit val session = new Cluster
    .Builder()
    .addContactPoints("localhost")
    .withPort(9042)
    .build()
    .connect()

  implicit def listenableFutureToFuture[T](
                                            listenableFuture: ListenableFuture[T]
                                          ): Future[T] = {
    val promise = Promise[T]()

    Futures.addCallback(listenableFuture, new FutureCallback[T] {
      def onFailure(error: Throwable): Unit = {
        promise.failure(error)
        ()
      }
      def onSuccess(result: T): Unit = {
        promise.success(result)
        ()
      }
    })
    promise.future
  }

  /*
  def execute(statement: Future[PreparedStatement], params: Any*)(
    implicit executionContext: ExecutionContext, session: Session
  ): Future[ResultSet] =
    statement
      .map(_.bind(params.map(_.asInstanceOf[Object])))
      .flatMap(session.executeAsync(_))
*/

  /*
    def execute(statement: Future[PreparedStatement], pageSize: Int, params: Any*)(
    implicit executionContext: ExecutionContext, session: Session
  ): Future[ResultSet] =
    for {
      ps <- statement
      bs =  ps.bind() //ps.bind(params.map(_.asInstanceOf[Object]))
      rs <- session.executeAsync(bs.setFetchSize(pageSize))
    } yield rs
  */

  def execute(statement: Future[PreparedStatement], pageSize: Int)(
    implicit executionContext: ExecutionContext, session: Session
  ): Future[ResultSet] =
    for {
      ps <- statement
      bs =  ps.bind() //ps.bind(params.map(_.asInstanceOf[Object]))
      rs <- session.executeAsync(bs.setFetchSize(pageSize))
    } yield rs




  implicit val ec = ExecutionContext.global

  val pID = 3
  val resultSet = execute(
    cql"select * from finance.currency",
    10
  )(ec,session)

  def fetchMoreResults(resultSet: ResultSet)(
    implicit executionContext: ExecutionContext, session: Session
  ): Future[ResultSet] =
    if (resultSet.isFullyFetched) {
      Future.failed(new NoSuchElementException("No more results to fetch"))
    } else {
      resultSet.fetchMoreResults()
    }

  val rowsFuture : Future[ResultSet] = resultSet


  val rowToCurrPair = (row: Row) => new CurrPair(row.getInt("id"),row.getString("name"))
  val computation = (rows: Seq[Row]) => rows.map(rowToCurrPair)

 // rowsFuture.map(computation)


/*
//DB query
val rowsFuture: Future[Seq[Row]] = cassandraSession.selectAll(readingFromTable)
val rowToString = (row: Row) => row.getString("name")
val computation = (rows: Seq[Row]) => rows.map(rowToString).mkString

// Computation to the data, rather than the other way around
val resultFuture = rowsFuture.map(computation)
*/


  /*
  * {
                        println("id=" + v.getInt("id") + " name=" + v.getString("name"))
                       }
  * */

/*
  for{r <- rows} {
    val thisRow1 = r.one()
    println("id="+thisRow1.getInt("id")+" name="+thisRow1.getString("name"))
    val thisRow2 = r.one()
    println("id="+thisRow2.getInt("id")+" name="+thisRow2.getString("name"))
    val thisRow3 = r.one()
    println("id="+thisRow3.getInt("id")+" name="+thisRow3.getString("name"))
  }
*/


/*
  """==============================================
    |
    |Here we go, failed to process for week
    |
    |===============================================
  """
  */


  Thread.sleep(5000)

  println("session.getState.getConnectedHosts="+session.getState.getConnectedHosts)
  session.close()
}
