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

  // Http().bindAndHandle(extractRequestRoute, "localhost", 8080)

  /*
  * Type 3: Composite directives
  */

  val simpleNestedRoute =
    path("api" / "item") {
      get {
        complete(StatusCodes.OK)
      }
    }

  // can be rewritten more concisely as
  // composite filtering directives - must match both conditions
  val compactSimpleNestedRoute = (path("api" / "item") & get) {
    complete(StatusCodes.OK)
  }

  // you can also include extraction directives
  val compactExtractRequestRoute =
    (path("controlEndpoint") & extractRequest & extractLog) { (request, log) =>
      log.info(s"I got the http request: $request")
      complete(StatusCodes.OK)
    }

  // you can group similar directives under a common directive
  // /about and /aboutUs
  val repeatedRoute =
    path("about") {
      complete(StatusCodes.OK)
    } ~
      path("aboutUs") {
        complete(StatusCodes.OK)
      }

  // you can concisely group these using |
  val dryRoute =
    (path("about") | path("aboutUs")) {
      complete(StatusCodes.OK)
    }

  // you can also use OR on extraction directives but only under certain conditions
  // yourblog.com/42 AND yourblog.com?postId=42

  val blogByIdRoute =
    path(IntNumber) { (blogpostId: Int) =>
      // do some server logic
      complete(StatusCodes.OK)
    }

  val blogByQueryParamRoute =
    parameter('postId.as[Int]) { (blogpostId: Int) =>
      // the same server logic
      complete(StatusCodes.OK)
    }

  // extracts either depending on shape of URI
  // must extract same kind and number of values
  val combinedBlogByIdRoute =
    (path(IntNumber) | parameter('postId.as[Int])) { (blogpostId: Int) =>
      // your original server logic
      complete(StatusCodes.OK)
    }

  /*
  * Type 4: "Actionable" Directives
  */
  // the complete directive takes an argument of ToResponseMarshallable - anything that can become a http response
  val completeOkRoute = complete(StatusCodes.OK)

  // failWith takes an exception and produces a 500 status code
  val failedRoute =
    path("notSupported") {
      failWith(new RuntimeException("Unsupported")) // completes with HTTP 500 status code
    }

  // reject hands over a request to the next possible handler in the routing tree
  // rejection happens automatically when a request doesn't match a directive
  val routeWithRejection = {
//    path("home") {
//      reject
//    } ~
    path("index") {
      completeOkRoute
    }
  }

  /*
   * Exercise
   */
  val getOrPutPath = {
  path("api" / "myEndpoint") {
    get {
      completeOkRoute
    } ~ // don't forget the ~ !!!
      post {
        complete(StatusCodes.Forbidden)
      }
  }

  Http().bindAndHandle(getOrPutPath, "localhost", 8081)
  }
}
