import play.api.mvc._

import controllers.CorsFilter

object Global extends WithFilters(CorsFilter) {
  // ...
}
