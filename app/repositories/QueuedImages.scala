package repositories

import scala.slick.driver.H2Driver.simple._

class QueuedImages(tag: Tag) extends Table[(Long, String, String, Long)](tag, "queued_images") {
  def id = column[Long]("created_at")

  def imageURL = column[String]("image_url")

  def sourceURL = column[String]("source_url")

  def createdAt = column[Long]("created_at")

  def * = (id, imageURL, sourceURL, createdAt)
}
