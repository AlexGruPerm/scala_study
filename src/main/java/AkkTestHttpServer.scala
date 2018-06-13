import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.HttpApp
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.ServerSettings
import com.typesafe.config.ConfigFactory
import akka.actor._

//https://doc.akka.io/docs/akka-http/current/routing-dsl/index.html#minimal-example

object WebServer extends HttpApp {

  override def routes: Route =
    path("hello") {
      //println(">"+WebServer.get)
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    }~
      path("admin") {
        //println(">"+WebServer.get)
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Mmmm are you admin?</h1>"))
        }
      }

}

object AkkTestHttpServer extends App{

  val system = ActorSystem("ownActorSystem")
  val settings = ServerSettings(ConfigFactory.load).withVerboseErrorMessages(true)
  WebServer.startServer("localhost", 8080, settings, system)
  system.terminate()

}
