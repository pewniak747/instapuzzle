package controllers

import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object CorsFilter extends Filter {
  def apply(nextFilter: (RequestHeader) => Future[SimpleResult])
    (requestHeader: RequestHeader): Future[SimpleResult] = {
      nextFilter(requestHeader).map { result =>
        result.withHeaders(
          "Access-Control-Allow-Origin" -> requestHeader.headers.get("Origin").getOrElse(""),
          "Access-Control-Allow-Credentials" -> "true"
        )
      }
    }
}
