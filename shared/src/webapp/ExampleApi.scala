package webapp

import scala.concurrent.Future

trait ExampleApi {

  def add(a: Int, b: Int): Future[Int]

}
