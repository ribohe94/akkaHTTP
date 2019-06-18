import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.concurrent.Future
import scala.io.StdIn
import scala.util.Random

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

object App extends App {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

  val numbers = Source.fromIterator(() => Iterator.continually(Random.nextInt()))

  final case class Item(name: String, id: Long)

  final case class Order(items: List[Item])

  var orders: List[Item] = (1L to 10L).map(Item("name", _)).toList

  implicit val itemFormat = jsonFormat2(Item)
  implicit val orderFormat = jsonFormat1(Order)

  def fetchItem(itemId: Long): Future[Option[Item]] = Future {
    orders.find(_.id == itemId)
  }

  val route = get {
    pathPrefix("item" / LongNumber) { id =>
      val maybeItem: Future[Option[Item]] = fetchItem(id)

      onSuccess(maybeItem) {
        case Some(item) => complete(item)
        case None => complete(StatusCodes.NotFound)
      }
    }
  }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())

}
