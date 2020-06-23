package webapp

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

  val app: Div = div(
    h1("Web App Example"),
    div(
      fontSize := "20px",
      strong("Hello, "),
      child.text <-- $currentName.signal
    ),
    button(onClick.map(_ => Uuid.v4()) --> $currentName.writer, "Update UUID")
  )

  def main(args: Array[String]): Unit = {
    val appContainer = dom.document.getElementById("appContainer")
    render(appContainer, app)
  }

}
