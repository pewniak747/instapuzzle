package repositories

import scala.slick.driver.H2Driver.simple._

class QueuedImages(tag: Tag) extends Table[(Long, String, Int, String, Long)](tag, "queued_images") {
  def id = column[Long]("created_at")

  def imageURL = column[String]("image_url")

  def size = column[Int]("size")

  def sourceURL = column[String]("source_url")

  def createdAt = column[Long]("created_at")

  def * = (id, imageURL, size, sourceURL, createdAt)
}
