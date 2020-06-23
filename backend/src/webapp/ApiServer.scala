package webapp

import upickle.default._

object ApiServer extends autowire.Server[String, Reader, Writer] {

  def write[Result: Writer](r: Result): String = upickle.default.write(r)

  def read[Result: Reader](p: String): Result = upickle.default.read[Result](p)

}
