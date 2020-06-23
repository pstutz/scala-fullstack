package webapp

import autowire._
import com.raquo.laminar.api.L._
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("uuid", JSImport.Default)
object Uuid extends js.Object {
  @js.native
  object v4 extends js.Object {
    def apply(): String = js.native
  }
}

object Frontend {

  val $currentName: Var[String] = Var("undefined")

  val $additionResult: EventStream[Int] = EventStream.fromFuture(ApiClient[ExampleApi].add(1, 2).call())

  val app: Div = div(
    h1("Web App Example"),
    div(
      strong("Hello, "),
      child.text <-- $currentName.signal,
    ),
    div(
      strong("Server computed addition of 1 + 2 = "),
      child.text <-- $additionResult.map(_.toString)
    ),
    button(onClick.map(_ => Uuid.v4()) --> $currentName.writer, "Update UUID")
  )

  def main(args: Array[String]): Unit = {
    val appContainer = dom.document.getElementById("appContainer")
    render(appContainer, app)
  }

}
