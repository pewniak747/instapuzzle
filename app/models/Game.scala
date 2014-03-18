package models

import akka.actor._
import scala.collection.mutable
import scala.concurrent.duration._
import faker.Name

import play.api.libs.concurrent.Execution.Implicits._

import controllers._

case class Player(id: String, name: String)

class Game(val broadcast: ActorRef) extends Actor with ActorLogging {

  var playersMap: mutable.Map[String, Player] = mutable.Map.empty

  var holders: mutable.Map[Piece, Player] = mutable.Map.empty

  var board: Option[Board] = None

  def receive = finished

  def started: Receive = players orElse {
    case PiecePickup(sessionId, pieceId) => {
      playersMap.get(sessionId).map { player =>
        val piece = Piece(pieceId)
        if (isValidPiece(piece) && canPickup(player, piece)) {
          if (!board.get.isAtCorrectPosition(piece)) {
            holders.put(piece, player)
            broadcast ! PiecePicked(piece, player)
          } else {
            sender ! PieceCorrect(piece)
          }
        }
      }
    }

    case PieceMove(sessionId, pieceId, x, y) => {
      playersMap.get(sessionId).map { player =>
        val piece = Piece(pieceId)
        val position = Position(x, y)
        for {
          board <- board;
          piece <- Some(piece) if isValidPiece(piece);
          targetPiece <- board.at(position);
          movingPlayer <- holders.get(piece) if player == movingPlayer
        } yield {
          if (!board.isAtCorrectPosition(targetPiece)) {
            for((movedPiece, movedPosition) <- board.move(piece, position)) yield {
              holders.remove(piece)
              broadcast ! PieceMoved(piece, position)
              broadcast ! PieceMoved(movedPiece, movedPosition)
              if (board.isFinished) {
                context.become(finished)
                holders.clear
                broadcast ! BoardFinished()
                context.system.scheduler.scheduleOnce(5.seconds) {
                  context.parent ! BoardRequest
                }
              }
            }
          } else {
            sender ! PieceCorrect(targetPiece)
          }
        }
      }
    }
  }

  def finished: Receive = players orElse {
    case BoardChange(newBoard: Board) => {
      board = Some(newBoard)
      context.become(started)
      broadcast ! BoardSynced(newBoard)
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
      board.map { board =>
        sender ! BoardSynced(board)
      }
    }
  }

  private def isValidPiece(piece: Piece) = board.get.pieces contains piece

  private def canPickup(player: Player, piece: Piece) = pieceHolder(piece) == None && holding(player) == None

  private def pieceHolder(piece: Piece) = holders.get(piece)

  private def holding(player: Player) = holders.map(_.swap).get(player)

}
