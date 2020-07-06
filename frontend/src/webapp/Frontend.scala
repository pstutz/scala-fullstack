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

  private val $currentUuid: Var[String] = Var(Uuid.v4())

  private val $additionResult: EventStream[Int] =
    EventStream.fromFuture(RPC.exampleApi.add(1, 2))

  private val app: Div = div(
    cls := "container mt-10 rounded overflow-hidden shadow-lg",
    div(
      cls := "px-8 py-6",
      div(cls := "font-bold text-xl", "Web App Example"),
      p(
        cls := "mt-4 text-gray-700 text-base",
        child.text <-- $additionResult.map(r => s"Addition RPC: 1 + 2 = $r")),
      p(
        cls := "mt-4 text-gray-700 text-base",
        child.text <-- $currentUuid.signal.map(uuid =>
          s"UUID computed by JavaScript library: $uuid")),
      button(
        cls := "mt-4 bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded",
        onClick.map(_ => Uuid.v4()) --> $currentUuid.writer,
        "Recompute UUID"
      )
    ),
  )

  def main(args: Array[String]): Unit = {
    val appContainer = dom.document.getElementById("appContainer")
    render(appContainer, app)
  }

}
