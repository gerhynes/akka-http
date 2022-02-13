package part3_highlevelserver

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, StatusCodes}
import akka.stream.ActorMaterializer

object DirectivesBreakdown extends App {
  implicit val system = ActorSystem("DirectivesBreakdown")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  import akka.http.scaladsl.server.Directives._

/*
* Type 1: Filtering Directives
* */
  val simpleHttpMethodRoute =
    post { // equivalent directives for GET, PUT, PATCH, DELETE
      complete(StatusCodes.Forbidden)
    }

  val simplePathRoute =
    path("about") {
      complete(
        HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   <h1>Hello form the about page</h1>
            | </body>
            |</html>
          """.stripMargin
        )
      )
    }

  val complexPathRoute =
    path("api" / "myEndpoint") { // /api/myEndpoint
      complete(StatusCodes.OK)
    }

  // Don't do this
  val dontConfuse =
    path("api/myEndpoint"){ // this string will get URL encoded as api%2FmyEndpoint
      complete(StatusCodes.OK)
    }

  val pathEndRoute =
    pathEndOrSingleSlash { // localhost:8080 or localhost:8080/
      complete(StatusCodes.OK)
    }

//  Http().bindAndHandle(complexPathRoute, "localhost", 8080)

  /*
  * Type 2: Extraction Directives
  */

  // GET /api/item/42
  val pathExtractionRoute =
    path("api" / "item" / IntNumber) { (itemNumber: Int) =>
      // other directives
      println(s"I've got a number in my path: $itemNumber")
      complete(StatusCodes.OK)
    }

  val pathMultiExtract =
    path("api" / "order" / IntNumber / IntNumber) { (orderId, itemId) =>
      println(s"I've got 2 numbers in my path: $orderId $itemId")
      complete(StatusCodes.OK)
    }

  val queryParamExtractionRoute = {
    // /api/item?id=45
    path("api" / "item") {
      // by default a string - but you can make it type safe
      // you can get performance benefits by accessing params as symbols
      parameter('id.as[Int]) { (itemId: Int) =>
        println(s"I've extracted the id as $itemId")
        complete(StatusCodes.OK)
      }
    }
  }

  val extractRequestRoute =
    path("controlEndpoint") {
      // You can extract the http request from the request context data structure
      extractRequest { (httpRequest: HttpRequest) =>
        extractLog { (log: LoggingAdapter) =>
          log.info(s"I got the http request: $httpRequest")
          complete(StatusCodes.OK)
        }
      }
    }

  Http().bindAndHandle(extractRequestRoute, "localhost", 8080)
}
