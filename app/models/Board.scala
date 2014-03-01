package models

import java.util.UUID
import scala.collection.mutable

case class Piece(val id: String) {
  require(id.size > 0)
}

case class Position(val x: Int, val y: Int) {
  require(x >= 0)
  require(y >= 0)
}

class Board(val imageURL: String, val width: Int, val height: Int) {
  require(imageURL.size > 0)
  require(width > 1)
  require(height > 1)

  val size = width * height
  val pieces: Set[Piece] = (1 to size).map { _ =>
    Piece(UUID.randomUUID.toString)
  }.toSet

  private val positions: Set[Position] = (for {
    y <- 0 until height;
    x <- 0 until width
  } yield Position(x, y)).toSet

  private var piecePositions = mutable.Map[Position, Piece]((positions zip pieces).toSeq:_*)

  def at(pos: Position): Option[Piece] = piecePositions.get(pos)

  def move(piece: Piece, position: Position) = {
    for {
      targetPiece <- at(position);
      source <- positionOf(piece)
    } yield {
      piecePositions.put(position, piece)
      piecePositions.put(source, targetPiece)
    }
  }

  private def positionOf(piece: Piece): Option[Position] = piecePositions.map(_.swap).get(piece)
}
