import com.datastax.driver.core._
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.implicitConversions
import scala.collection.JavaConverters._
import scala.util.{Success, Failure}





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
    cql"SELECT * FROM testkeyspace.person",
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

  val rows : Future[ResultSet] = resultSet

  for{r <- rows} {
    println("id="+r.one().getInt("id"))
  }


  /*
  rows.map {r => r.one()} onComplete {
    case Success(v) => {
                        //println(v.getColumnDefinitions)
                         val rowID    = v.getInt("id")
                         val rowName = v.getString("name")
                         println( "id = "+rowID+" name = ["+rowName+"]")
                       }
    case Failure(e) => e.printStackTrace()
  }
  */

  /*
  rows.map {r => r.one().getInt(0)} onComplete {
    case Success(v) => println(v)
    case Failure(e) => e.printStackTrace()
  }
  */

  /*
  rows.map {r => r.one().getInt(0)} onComplete {
    case Success(v) => println(s"Id = $v")
    case Failure(e) => e.printStackTrace()
  }
  */

  /*
  val rows: Future[Iterable[Row]] = resultSet.map(_.asScala)

 for(r <- rows)  {
   r.get
    for (f <- r.toList){
      println("f="+f.toString)
    }
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
