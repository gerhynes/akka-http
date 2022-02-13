package part3_highlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

object HighLevelIntro extends App {
  implicit val system = ActorSystem("HighLevelIntro")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  // Directives specify what happens under what conditions
  import akka.http.scaladsl.server.Directives._

  val simpleRoute: Route =
    path("home") { // directive
      complete(StatusCodes.OK) // directive
    }

  val pathGetRoute: Route =
    path("home") {
      get {
        complete(StatusCodes.OK)
      }
    }

  // Chaining directives with ~
  val chainedRoute: Route =
  path("myEndpoint") {
    get {
      complete(StatusCodes.OK)
    } ~
      post {
        complete(StatusCodes.Forbidden)
      }
  } ~
    path("home") {
      complete(
        HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   <h1>Hello from the high-level API</h1>
            | </body>
            |</html>
          """.stripMargin
        )
      )
    } // Routing tree

  Http().bindAndHandle(chainedRoute, "localhost", 8080)
}
