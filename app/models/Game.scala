package models

import akka.actor._
import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.Random
import faker.Name

import play.api.libs.concurrent.Execution.Implicits._

import controllers._

case class Player(id: String, name: String)

case object BoardChange

class Game(val broadcast: ActorRef) extends Actor with ActorLogging {

  var playersMap:  mutable.Map[String, Player] = mutable.Map.empty

  var holders:   mutable.Map[Piece, Player]  = mutable.Map.empty

  val images = List(
    Image("http://distilleryimage6.ak.instagram.com/6191e200971711e3bad01215527ad906_8.jpg", "http://instagram.com/p/kewEAyOjTR"),
    Image("http://distilleryimage7.ak.instagram.com/57a2ac147ed211e3962a12bf16838833_8.jpg", "http://instagram.com/p/jPQOf6u6ez"),
    Image("http://distilleryimage3.ak.instagram.com/ccb35e56a6c111e38aca0e51ab9d14d3_8.jpg", "http://instagram.com/p/lSFbkQgr8N"),
    Image("http://distilleryimage3.ak.instagram.com/992f8acca6c211e38f0d1262a4bedf57_8.jpg", "http://instagram.com/p/lSGAzqFN5C")
  )

  val sizes = List((5, 5), (6, 6), (8, 8), (10, 10))

  var board: Board = newBoard

  def receive = started

  def started: Receive = players orElse {
    case PiecePickup(sessionId, pieceId) => {
      playersMap.get(sessionId).map { player =>
        val piece = Piece(pieceId)
        if (isValidPiece(piece) && pieceHolder(piece) == None) {
          holders.put(piece, player)
          broadcast ! PiecePicked(player, pieceId)
        }
      }
    }

    case PieceMove(sessionId, pieceId, x, y) => {
      playersMap.get(sessionId).map { player =>
        val piece = Piece(pieceId)
        val position = Position(x, y)
        for {
          piece <- Some(piece) if isValidPiece(piece);
          movingPlayer <- holders.get(piece) if player == movingPlayer;
          (movedPiece, movedPosition) <- board.move(piece, position)
        } yield {
          holders.remove(piece)
          broadcast ! PieceMoved(piece.id, position)
          broadcast ! PieceMoved(movedPiece.id, movedPosition)
          if (board.isFinished) {
            context.become(finished)
            broadcast ! BoardFinished()
            context.system.scheduler.scheduleOnce(5.seconds) {
              self ! BoardChange
            }
          }
        }
      }
    }
  }

  def finished: Receive = players orElse {
    case BoardChange => {
      board = newBoard
      context.become(started)
      broadcast ! BoardSynced(board)
    }
  }

  def players: Receive = {
    case PlayerJoin(sessionId) => {
      val id = sessionId
      val name = Name.name
      val player = Player(id, name)
      playersMap.put(sessionId, player)
      sender ! PlayerLogin(player)
      broadcast ! PlayerJoined(id, name)
    }

    case PlayerLeave(sessionId) => {
      playersMap.remove(sessionId).map { player =>
        holders.filter { case (piece, holder) => player == holder }.map { case (piece, _) => holders.remove(piece) }
        broadcast ! PlayerLeft(player.id)
      }
    }

    case PlayersSync(sessionId) => {
      sender ! PlayersSynced(playersMap.values.toList)
    }

    case BoardSync(sessionId) => {
      sender ! BoardSynced(board)
    }
  }

  private def isValidPiece(piece: Piece) = board.pieces contains piece

  private def pieceHolder(piece: Piece) = holders.get(piece)

  private def newBoard: Board = {
    val image = Random.shuffle(images).head
    val (width, height) = Random.shuffle(sizes).head
    board = new Board(image, width, height)
    board.shuffle
    board
  }

}
