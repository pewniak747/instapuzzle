import org.specs2.mutable._

import models.Board
import models.Piece
import models.Position

class BoardSpec extends Specification {

  "Board" should {

    val board = new Board("image-url", 3, 2)

    "have dimensions" in {

      "width" in {
        board.width must equalTo(3)
      }

      "height" in {
        board.height must equalTo(2)
      }

    }

    "have image URL" in {
      board.imageURL must equalTo("image-url")
    }

    "have width x height pieces" in {
      board.pieces.size must equalTo(6)
    }

    "at" in {

      "valid position on board" in {
        board.at(Position(2, 1)) must beLike { case Some(Piece(_)) => ok }
      }

      "position outsize of width" in {
        board.at(Position(3, 1)) must equalTo(None)
      }

      "position outside of height" in {
        board.at(Position(2, 2)) must equalTo(None)
      }

    }

    "move" in {

      "valid piece and source" in {

        val source = Position(0, 0)
        val target = Position(1, 1)
        val piece = board.at(source).get
        val targetPiece = board.at(target).get

        board.move(piece, target)

        "moves piece to target position" in {
          board.at(target) must equalTo(Some(piece))
        }

        "moves targetPiece to source position" in {
          board.at(source) must equalTo(Some(targetPiece))
        }

      }

      "invalid target" in {
        val source = Position(0, 0)
        val target = Position(2, 2)
        val piece = board.at(source).get

        "does not move the piece" in {
          board.move(piece, target)
          board.at(source) must equalTo(Some(piece))
          board.at(target) must equalTo(None)
        }
      }

      "invalid piece" in {
        val target = Position(1, 1)
        val piece = Piece("invalid")
        val targetPiece = board.at(target).get

        "does not move the piece" in {
          board.move(piece, target)
          board.at(target) must equalTo(Some(targetPiece))
        }

      }

    }

  }
}
