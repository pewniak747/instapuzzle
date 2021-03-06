package models

import play.api.mvc._
import play.api.libs.json._

class Serializer extends Writes[OutgoingEvent] {
  def writes(event: OutgoingEvent) = event match {
    case PlayerJoined(id, name) => Json.obj(
      "name" -> "player:joined",
      "args" -> Json.arr(Json.obj(
        "id" -> id,
        "name" -> name
      ))
    )
    case PlayersSynced(players) => Json.obj(
      "name" -> "player:synced",
      "args" -> Json.arr(Json.toJson(
        players.map { player => Json.obj(
          "id" -> player.id,
          "name" -> player.name
        ) }.toList
      ))
    )
    case PlayerLeft(id) => Json.obj(
      "name" -> "player:left",
      "args" -> Json.arr(Json.obj(
        "id" -> id
      ))
    )
    case PlayerLogin(player) => Json.obj(
      "name" -> "player:login",
      "args" -> Json.arr(Json.obj(
        "id" -> player.id
      ))
    )
    case BoardSynced(board) => Json.obj(
      "name" -> "board:synced",
      "args" -> Json.arr(Json.obj(
        "image" -> Json.obj(
          "url" -> board.image.url,
          "source" -> board.image.sourceUrl
        ),
        "width" -> board.width,
        "height" -> board.height,
        "finished" -> board.isFinished,
        "pieces" -> Json.toJson(
          board.positions.map { position =>
            Json.obj(
              "id" -> board.at(position).get.id,
              "x" -> position.x,
              "y" -> position.y
            )
          }
        )
      ))
    )
    case BoardFinished() => Json.obj(
      "name" -> "board:finished",
      "args" -> Json.arr()
    )
    case PiecePicked(piece, player) => Json.obj(
      "name" -> "piece:picked",
      "args" -> Json.arr(Json.obj(
        "player_id" -> player.id,
        "piece_id" -> piece.id
      ))
    )
    case PieceCorrect(piece) => Json.obj(
      "name" -> "piece:correct",
      "args" -> Json.arr(Json.obj(
        "piece_id" -> piece.id
      ))
    )
    case PieceMoved(piece, position) => Json.obj(
      "name" -> "piece:moved",
      "args" -> Json.arr(Json.obj(
        "piece_id" -> piece.id,
        "position" -> Json.obj(
          "x" -> position.x,
          "y" -> position.y
        )
      ))
    )
    case _ => Json.obj()
  }
}
