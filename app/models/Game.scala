package models

import akka.actor._

import scala.collection.mutable

import controllers._

case class Player(id: String, name: String)

class Game(val broadcast: ActorRef) extends Actor with ActorLogging {

  var playersMap:  mutable.Map[String, Player] = mutable.Map.empty

  var holders:   mutable.Map[Piece, Player]  = mutable.Map.empty

  var board = new Board("http://distilleryimage6.ak.instagram.com/6191e200971711e3bad01215527ad906_8.jpg", 5, 5)

  board.shuffle

  def receive = {

    case PlayerJoin(sessionId) => {
      val id = sessionId
      val name = "random name"
      playersMap.put(sessionId, Player(id, name))
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
