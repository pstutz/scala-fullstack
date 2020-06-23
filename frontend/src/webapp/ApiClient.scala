package webapp

import org.scalajs.dom
import upickle.default._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

object ApiClient extends autowire.Client[String, Reader, Writer] {
  override def doCall(req: Request): Future[String] = {
    dom.ext.Ajax.post(
      url = "/api/" + req.path.mkString("/"),
      data = upickle.default.write(req.args.toSeq)
    ).map { response =>
      upickle.default.read[String](response.responseText)
    }
  }

  def read[Result: Reader](p: String): Result = upickle.default.read[Result](p)

  def write[Result: Writer](r: Result): String = upickle.default.write(r)
}
