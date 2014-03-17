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
    Image("http://distilleryimage0.ak.instagram.com/da5da88ca91311e3b145120e7295bf20_8.jpg", "http://instagram.com/p/lZsC2anc1C"),
    Image("http://distilleryimage8.ak.instagram.com/3af3b0c2a91111e38d6c129f0cea75bc_8.jpg", "http://instagram.com/p/lZp8eKK35z"),
    Image("http://distilleryimage0.ak.instagram.com/0f69c3aca91511e3b2630e9c0aaa8964_8.jpg", "http://instagram.com/p/lZs_zHuaYP"),
    Image("http://distilleryimage4.ak.instagram.com/1c298576a90d11e3b4de12ff50c9f474_8.jpg", "http://instagram.com/p/lZme-zAm-B"),
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
        if (isValidPiece(piece) && canPickup(player, piece)) {
          if (!board.isAtCorrectPosition(piece)) {
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
                  self ! BoardChange
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

  private def canPickup(player: Player, piece: Piece) = pieceHolder(piece) == None && holding(player) == None

  private def pieceHolder(piece: Piece) = holders.get(piece)

  private def holding(player: Player) = holders.map(_.swap).get(player)

  private def newBoard: Board = {
    val image = Random.shuffle(images).head
    val (width, height) = Random.shuffle(sizes).head
    board = new Board(image, width, height)
    board.shuffle
    board
  }

}
