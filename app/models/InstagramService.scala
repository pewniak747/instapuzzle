package models

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._

import dispatch._, Defaults._

class InstagramService(val clientId: String) {
  def popular = {
    val request = url(instagramEndpoint).addQueryParameter("client_id", clientId)
    val response = Http(request OK as.String)(defaultContext)()
    val json = Json.parse(response)
    val data = (json \ "data").as[Seq[JsObject]]
    val imagesData = data.filter(j => (j \ "type").as[String] == "image")
                          .map(j => ((j \ "images" \ "standard_resolution" \ "url").as[String], (j \ "link").as[String]))
    imagesData.map(Image.tupled(_))
  }

  private val instagramEndpoint = "https://api.instagram.com/v1/media/popular"
}
