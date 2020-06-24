package webapp

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.{Http2ServerSettings, ServerSettings}
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

object WebServer {

  def main(args: Array[String]): Unit = {
    val config: Config = ConfigFactory.load()

    implicit val system: ActorSystem = ActorSystem("server-system", config)
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    val serverFuture: Future[Http.ServerBinding] = Http().bindAndHandleAsync(
      handler = Route.asyncHandler(new Router().routes),
      interface = "localhost",
      settings = ServerSettings(config).withHttp2Settings(Http2ServerSettings(config))
    )
    val server = Await.result(serverFuture, Duration.Inf)
    val host = server.localAddress.getAddress.getHostName
    val port = server.localAddress.getPort

    println(s"Server online at http://$host:$port")
  }
}
