package models

import java.util.UUID
import scala.util.Random
import scala.collection.mutable
import scala.collection.concurrent

case class Piece(val id: String) {
  require(id.size > 0)
}

case class Position(val x: Int, val y: Int) {
  require(x >= 0)
  require(y >= 0)
}

case class Image(val url: String, val sourceUrl: String) {
  require(url.size > 0)
}

class Board(val image: Image, val width: Int, val height: Int) {
  require(width > 1)
  require(height > 1)

  val positions: List[Position] = (for {
    y <- 0 until height;
    x <- 0 until width
  } yield Position(x, y)).toList

  val size = width * height

  val pieces: List[Piece] = positions.map { position =>
    val x = position.x
    val y = position.y
    val uuid = UUID.randomUUID.toString
    Piece(s"$x:$y:$uuid")
  }.toList

  def at(pos: Position): Option[Piece] = piecePositions.get(pos)

  def isAtCorrectPosition(piece: Piece) = positionOf(piece) match {
    case Some(position) => correctPositions(position) == piece
    case _ => false
  }

  def move(piece: Piece, target: Position) = {
    for {
      targetPiece <- at(target);
      source <- positionOf(piece)
    } yield {
      piecePositions ++= Map(target -> piece, source -> targetPiece)
      (targetPiece, source)
    }
  }

  def isFinished = positions.forall { position =>
    piecePositions(position) == correctPositions(position)
  }

  def shuffle = do {
    val shuffledPieces = Random.shuffle(pieces)
    piecePositions = concurrent.TrieMap((positions zip shuffledPieces).toSeq:_*)
  } while (isFinished)

  private var correctPositions = (positions zip pieces).toMap

  private var piecePositions = concurrent.TrieMap[Position, Piece](correctPositions.toSeq:_*)

  private def positionOf(piece: Piece): Option[Position] = piecePositions.map(_.swap).get(piece)

}
