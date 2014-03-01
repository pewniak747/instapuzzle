package models

import java.util.UUID
import scala.util.Random
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

  def at(pos: Position): Option[Piece] = piecePositions.get(pos)

  def move(piece: Piece, target: Position) = {
    for {
      targetPiece <- at(target);
      source <- positionOf(piece)
    } yield {
      piecePositions.put(target, piece)
      piecePositions.put(source, targetPiece)
      (targetPiece, source)
    }
  }

  def isFinished = positions.forall { position =>
    piecePositions(position) == correctPositions(position)
  }

  def shuffle = do {
    val shuffledPieces = Random.shuffle(pieces.toList)
    piecePositions = mutable.Map((positions zip shuffledPieces).toSeq:_*)
  } while (isFinished)

  private val positions: Set[Position] = (for {
    y <- 0 until height;
    x <- 0 until width
  } yield Position(x, y)).toSet

  private var correctPositions = (positions zip pieces).toMap

  private var piecePositions = mutable.Map[Position, Piece](correctPositions.toSeq:_*)

  private def positionOf(piece: Piece): Option[Position] = piecePositions.map(_.swap).get(piece)

}
