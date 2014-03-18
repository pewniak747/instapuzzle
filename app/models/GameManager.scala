package models

import akka.actor._

case class BoardChange(board: Board)

case object BoardRequest

case class BoardResponse(image: Image, dimensions: (Int, Int))

class GameManager(broadcast: ActorRef) extends Actor with ActorLogging {

  val game = context.actorOf(Props(new Game(broadcast)), name = "game")

  val boards = context.actorOf(Props[BoardsManager])

  def receive = {
    case BoardRequest => {
      boards ! BoardRequest
    }

    case BoardResponse(image, (width, height)) => {
      val board = new Board(image, width, height)
      board.shuffle
      game ! BoardChange(board)
    }

    case event@_ => game.forward(event)
  }

  override def preStart = self ! BoardRequest

}
