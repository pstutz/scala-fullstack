package webapp

import java.nio.ByteBuffer
import boopickle.Default._
import chameleon.ext.boopickle._
import covenant.http.HttpClient
import sloth.{Client, ClientException}
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object RPC {
  private val client: Client[ByteBuffer, Future, ClientException] = HttpClient[ByteBuffer]("/api")
  val exampleApi: ExampleApi = client.wire[ExampleApi]
}
