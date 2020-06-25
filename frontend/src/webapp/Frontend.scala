package webapp

import com.raquo.laminar.api.L._
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("./styles.css", JSImport.Namespace)
object Css extends js.Object

@js.native
@JSImport("uuid", JSImport.Default)
object Uuid extends js.Object {
  @js.native
  object v4 extends js.Object {
    def apply(): String = js.native
  }
}

object Frontend {
  private val css = Css

  private val $currentUuid: Var[String] = Var("- push button to compute -")

  private val $additionResult: EventStream[Int] = EventStream.fromFuture(RPC.exampleApi.add(1, 2))

  private val app: Div = div(
    h1("Web App Example"),
    div(
      cls := "text-xl",
      strong("UUID computed by JavaScript library: "),
      child.text <-- $currentUuid.signal,
    ),
    div(
      strong("Server computed addition of 1 + 2 = "),
      child.text <-- $additionResult.map(_.toString)
    ),
    button(onClick.map(_ => Uuid.v4()) --> $currentUuid.writer, "Compute UUID")
  )

  def main(args: Array[String]): Unit = {
    val appContainer = dom.document.getElementById("appContainer")
    render(appContainer, app)
  }

}
