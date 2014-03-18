package models

import akka.actor._

import repositories.QueuedImagesRepo

class BoardsManager extends Actor with ActorLogging {

  def receive = {
    case BoardRequest => {
      val queuedImage = QueuedImagesRepo.sample
      val image = queuedImage.image
      val dimensions = (queuedImage.size, queuedImage.size)
      sender ! BoardResponse(image, dimensions)
    }
  }

}
