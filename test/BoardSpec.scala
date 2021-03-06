import org.specs2.mutable._
import org.specs2.specification._

import models.Image
import models.Board
import models.Piece
import models.Position

class BoardSpec extends Specification {

  trait WithBoard extends NameSpace {
    val image = Image("image-url", "")
    val board = new Board(image, 3, 2)
  }

  "Board" should {

    "have dimensions" in new WithBoard {

      "width" in {
        board.width must equalTo(3)
      }

      "height" in {
        board.height must equalTo(2)
      }

    }

    "have image URL" in new WithBoard {
      board.image.url must equalTo("image-url")
    }

    "have width x height pieces" in new WithBoard {
      board.pieces.size must equalTo(6)
    }

    "at" in new WithBoard {

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

    "move" in new WithBoard {

      "valid piece and source" in {

        val source = Position(0, 0)
        val target = Position(1, 1)
        val piece = board.at(source).get
        val targetPiece = board.at(target).get
        val result = board.move(piece, target)

        "moves piece to target position" in {
          board.at(target) must equalTo(Some(piece))
        }

        "moves targetPiece to source position" in {
          board.move(piece, target)
          board.at(source) must equalTo(Some(targetPiece))
        }

        "returns pair representing swapped piece" in {
          result must equalTo(Some((targetPiece, source)))
        }

      }

      "invalid target" in new WithBoard {
        val source = Position(0, 0)
        val target = Position(2, 2)
        val piece = board.at(source).get
        val result = board.move(piece, target)

        "does not move the piece" in {
          board.at(source) must equalTo(Some(piece))
          board.at(target) must equalTo(None)
        }

        "returns None" in {
          result must equalTo(None)
        }
      }

      "invalid piece" in new WithBoard {
        val target = Position(1, 1)
        val piece = Piece("invalid")
        val targetPiece = board.at(target).get
        val result = board.move(piece, target)

        "does not move the piece" in {
          board.at(target) must equalTo(Some(targetPiece))
        }

        "returns None" in {
          result must equalTo(None)
        }

      }

    }

    "isFinished" in new WithBoard {

      "is true for new board" in {
        board.isFinished must beTrue
      }

      "for disrupted board" in new WithBoard {

        board.move(board.at(Position(1, 1)).get, Position(0, 0))

        "is false" in {
          board.isFinished must beFalse
        }

      }

      "for solved board" in new WithBoard {
        board.move(board.at(Position(1, 1)).get, Position(0, 0))
        board.move(board.at(Position(0, 0)).get, Position(1, 1))

        "is true" in {
          board.isFinished must beTrue
        }

      }

    }

    "isAtCorrectPosition" in new WithBoard {

      val piece = board.pieces.head

      "is true for piece in correct position" in {
        board.isAtCorrectPosition(piece) must beTrue
      }

      "is false for piece in incorrect position" in new WithBoard {
        board.move(piece, Position(1, 1))
        board.isAtCorrectPosition(piece) must beFalse
      }
    }

    "shuffle" in new WithBoard {

      "makes board not finished" in {
        board.shuffle
        board.isFinished must beFalse
      }

    }

  }

}
