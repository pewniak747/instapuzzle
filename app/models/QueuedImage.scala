package models

case class QueuedImage(val id: Long, val createdAt: Long, val size: Int, val image: Image)
