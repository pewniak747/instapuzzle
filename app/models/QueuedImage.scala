package models

case class QueuedImage(val id: Option[Long], val createdAt: Long, val size: Int, val image: Image)
