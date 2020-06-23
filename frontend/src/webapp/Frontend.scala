package webapp

import com.thoughtworks.binding.Binding._
import org.lrng.binding.html
import org.lrng.binding.html.NodeBinding
import org.scalajs.dom.document
import org.scalajs.dom.raw._

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

  val uuid: Var[String] = Var("world")

  @html def root: NodeBinding[HTMLDivElement] = <div>
    <p>Hello {uuid.bind}!</p>
    <button onclick={_: Event => uuid.value = Uuid.v4()}>Update UUID</button>
  </div>

  def main(args: Array[String]): Unit = {
    html.render(document.body, root)
  }

}
