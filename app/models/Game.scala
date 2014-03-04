package models

import akka.actor._
import scala.collection.mutable
import faker.Name

import controllers._

case class Player(id: String, name: String)

class Game(val broadcast: ActorRef) extends Actor with ActorLogging {

  var playersMap:  mutable.Map[String, Player] = mutable.Map.empty

  var holders:   mutable.Map[Piece, Player]  = mutable.Map.empty

  var board = new Board("http://distilleryimage8.s3.amazonaws.com/106802c4a3b011e3bad2124a1fe00c8c_8.jpg", 8, 8)

  board.shuffle

  def receive = {

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
        }
      }
    }
  }

  private def isValidPiece(piece: Piece) = board.pieces contains piece

  private def pieceHolder(piece: Piece) = holders.get(piece)

}
