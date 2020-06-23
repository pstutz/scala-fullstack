package webapp

import akka.http.scaladsl.coding.Gzip
import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity}
import akka.http.scaladsl.server.{Directives, Route}

import scala.concurrent.ExecutionContext.Implicits.global

object Router extends Directives {

  private def httpEntity(content: String): HttpEntity.Strict = HttpEntity(ContentTypes.`text/html(UTF-8)`, content)

  private val indexHtml = httpEntity(os.read(os.resource / "public" / "index.html"))
  private val webpackBundle = httpEntity(os.read(os.resource / "out-bundle.js"))
  private val webpackSourceMap = httpEntity(os.read(os.resource / "out-bundle.js.map"))

  val route: Route = encodeResponseWith(Gzip) {
    pathSingleSlash {
      complete(indexHtml)
    } ~ path("out-bundle.js") {
      complete(webpackBundle)
    } ~ path("out-bundle.js.map") {
      complete(webpackSourceMap)
    } ~ post {
    path("api" / Segments){ s =>
      extract(_.request.entity match {
          case HttpEntity.Strict(nb: ContentType.NonBinary, data) =>
            data.decodeString(nb.charset.value)
          case _ => throw new UnsupportedOperationException(
            "Malformed API request, expected `HttpEntity.Strict(nb: ContentType.NonBinary, data)`")
        }) { serializedParameters =>
          complete {
            ApiServer.route[ExampleApi](ExampleApiImpl)(
              autowire.Core.Request(s, upickle.default.read[Seq[(String, String)]](serializedParameters).toMap)
            ).map(upickle.default.write(_))
          }
        }
      }
    }
  }

}
