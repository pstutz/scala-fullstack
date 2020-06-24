package webapp

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.{Http2ServerSettings, ServerSettings}
import akka.http.scaladsl.{Http, HttpConnectionContext}
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

object WebServer {
  val enableHttp2 = "akka.http.server.preview.enable-http2 = on"

  def main(args: Array[String]): Unit = {
    val config: Config = ConfigFactory
      .parseString(enableHttp2)
      .withFallback(ConfigFactory.load())

    implicit val system: ActorSystem = ActorSystem("server-system", config)
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    val interface = config.getString("http.interface")
    val port = config.getInt("http.port")
    val serverFuture: Future[Http.ServerBinding] = Http().bindAndHandleAsync(
      handler = Route.asyncHandler(new Router().routes),
      interface = interface,
      port = port,
      connectionContext = HttpConnectionContext(),
      settings = ServerSettings(config).withHttp2Settings(Http2ServerSettings(config)),
      parallelism = Runtime.getRuntime.availableProcessors()
    )
    Await.result(serverFuture, Duration.Inf)

    println(s"Server online at http://$interface:$port")
  }
}
