package webapp

import java.nio.ByteBuffer

import akka.http.scaladsl.coding.Gzip
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.{Directives, Route}
import boopickle.Default._
import cats.implicits._
import chameleon.ext.boopickle._
import covenant.http.AkkaHttpRoute
import covenant.http.ByteBufferImplicits._

import scala.concurrent.{ExecutionContext, Future}

class Router(implicit ec: ExecutionContext) extends Directives {

  private val indexHtml = httpEntity(os.read(os.resource / "public" / "index.html"))
  private val webpackBundle = httpEntity(os.read(os.resource / "out-bundle.js"))
  private val webpackSourceMap = httpEntity(os.read(os.resource / "out-bundle.js.map"))

  private val staticRoutes = encodeResponseWith(Gzip) {
    pathSingleSlash {
      complete(indexHtml)
    } ~ path("out-bundle.js") {
      complete(webpackBundle)
    } ~ path("out-bundle.js.map") {
      complete(webpackSourceMap)
    }
  }

  private val apiRouter = sloth.Router[ByteBuffer, Future].route[ExampleApi](ExampleApiImpl)
  private val apiRoutes = pathPrefix("api")(AkkaHttpRoute.fromFutureRouter(apiRouter))
  private def httpEntity(content: String): HttpEntity.Strict = HttpEntity(ContentTypes.`text/html(UTF-8)`, content)

  val routes: Route = staticRoutes ~ apiRoutes

}
