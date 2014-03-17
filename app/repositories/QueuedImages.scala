package repositories

import scala.slick.driver.H2Driver.simple._
import play.api.db.slick.DB
import play.api.Play.current

import models.{QueuedImage,Image}

class QueuedImages(tag: Tag) extends Table[QueuedImage](tag, "queued_images") {
  def id = column[Long]("created_at")

  def imageURL = column[String]("image_url")

  def size = column[Int]("size")

  def sourceURL = column[String]("source_url")

  def createdAt = column[Long]("created_at")

  def * = (id, imageURL, size, sourceURL, createdAt) <> (map _, unmap _)

  def map(tuple: (Long, String, Int, String, Long)) = tuple match {
    case (id: Long, imageURL: String, size: Int, sourceURL: String, createdAt: Long) =>
      QueuedImage(id, createdAt, size, Image(imageURL, sourceURL))
  }

  def unmap(queuedImage: QueuedImage) =
    Some((queuedImage.id, queuedImage.image.url, queuedImage.size, queuedImage.image.sourceUrl, queuedImage.createdAt))
}

object QueuedImagesRepo {
  def sample: QueuedImage = DB.withSession { implicit s =>
    table.first
  }

  val table = TableQuery[QueuedImages]
}
