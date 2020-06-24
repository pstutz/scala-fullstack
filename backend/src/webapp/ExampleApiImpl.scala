package webapp

import scala.concurrent.Future

object ExampleApiImpl extends ExampleApi {
  def add(a: Int, b: Int): Future[Int] = Future.successful(a + b)
}
