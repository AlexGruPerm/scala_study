import com.datastax.driver.core._
import com.google.common.util.concurrent.{ FutureCallback, Futures, ListenableFuture }
import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.language.implicitConversions
import scala.collection.JavaConverters._
//import ExecutionContext.Implicits.global



object CassandraWriteRead extends App {
  println("CassandraWriteRead examples")

  implicit class CqlStrings(val context: StringContext) extends AnyVal {
    def cql(args: Any*)(implicit session: Session): Future[PreparedStatement] = {
      val statement = new SimpleStatement(context.raw(args: _*))
      session.prepareAsync(statement)
    }

    def execute(statement: Future[PreparedStatement], params: Any*)(
      implicit executionContext: ExecutionContext, session: Session
    ): Future[ResultSet] = statement.map(_.bind(params.map(_.asInstanceOf[Object]))).flatMap(session.executeAsync(_))


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

  }


  implicit val session = new Cluster
  .Builder()
    .addContactPoints("localhost")
    .withPort(9042)
    .build()
    .connect()


  // val statement = cql"SELECT * FROM testkeyspace.person"
  // val resultSet = execute(statement)
  //val resultSet =  execute(cql"SELECT * FROM testkeyspace.person where id=?")
  //val rows: Future[Iterable[Row]] = resultSet.map(_.asScala)

  val pID = 1
  val resultSet = execute(cql"SELECT * FROM testkeyspace.person where id=?", pID)

  println("session.getState.getConnectedHosts="+session.getState.getConnectedHosts)
  session.close()
}

