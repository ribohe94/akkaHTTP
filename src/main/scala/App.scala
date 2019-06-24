import java.sql.Date

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.io.StdIn
import scala.math.Ordering.Implicits._


object App extends App {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

  val data = List(
    Map("group" -> "broadcaster", "user" -> "user01", "date" -> Date.valueOf("2019-06-10")),
    Map("group" -> "advertiser", "user" -> "user01", "date" -> Date.valueOf("2019-06-10")),
    Map("group" -> "ad-agency", "user" -> "user01", "date" -> Date.valueOf("2019-06-10")),
    Map("group" -> "broadcast", "user" -> "user01", "date" -> Date.valueOf("2019-06-10"))
  )


  def heavyQuery(group: String, metric: String, startDate: String, endDate: String): String = {
    val filteredData = data.filter(
      map => map("group") == group &&
        map("date").asInstanceOf[Date] >= Date.valueOf(startDate) &&
        map("date").asInstanceOf[Date] <= Date.valueOf(endDate)
    )
    s"""{"size":${filteredData.size}}"""
  }

  val route = get {
    path("report") {
      get {
        parameters(
          'group.as[String],
          'metric.as[String],
          'startDate.as[String],
          'endDate.as[String]
        ) {
          (group, metric, startDate, endDate) =>
            complete(HttpEntity(ContentTypes.`application/json`, heavyQuery(group, metric, startDate, endDate)))
        }
      }
    }
  }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())

}
