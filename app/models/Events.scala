package models

class Event

case class PlayerJoin(sessionId: String) extends Event

case class PlayerJoined(id: String, name: String) extends Event

case class PlayerLeave(sessionId: String) extends Event

case class PlayerLeft(id: String) extends Event

case class PlayersSync extends Event

case class PlayersSynced(players: Seq[Player]) extends Event

case class BoardSync extends Event

case class BoardSynced(board: Board) extends Event

case class PiecePickup(sessionId: String, pieceId: String) extends Event

case class PiecePicked(player: Player, pieceId: String) extends Event

case class PieceMove(sessionId: String, pieceId: String) extends Event

case class PieceMoved(pieceId: String, position: Position) extends Event
