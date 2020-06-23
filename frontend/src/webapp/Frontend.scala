package webapp

import org.scalajs.dom.document

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

  def main(args: Array[String]): Unit = {
    val element = document.createElement("p")
    val textNode = document.createTextNode(s"Hello ${Uuid.v4()}!")
    element.appendChild(textNode)
    document.body.appendChild(element)
  }

}
