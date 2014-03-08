package models

class Event

class IncomingEvent(sessionId: String) extends Event

case class PlayerJoin(sessionId: String) extends IncomingEvent(sessionId)

case class PlayerLeave(sessionId: String) extends IncomingEvent(sessionId)

case class PlayersSync(sessionId: String) extends IncomingEvent(sessionId)

case class BoardSync(sessionId: String) extends IncomingEvent(sessionId)

case class PiecePickup(sessionId: String, pieceId: String) extends IncomingEvent(sessionId)

case class PieceMove(sessionId: String, pieceId: String, x: Int, y: Int) extends IncomingEvent(sessionId)

class OutgoingEvent extends Event

case class PlayerJoined(id: String, name: String) extends OutgoingEvent

case class PlayerLeft(id: String) extends OutgoingEvent

case class PlayersSynced(players: Seq[Player]) extends OutgoingEvent

case class BoardSynced(board: Board) extends OutgoingEvent

case class BoardFinished extends OutgoingEvent

case class PiecePicked(player: Player, pieceId: String) extends OutgoingEvent

case class PieceMoved(pieceId: String, position: Position) extends OutgoingEvent

case class PlayerLogin(player: Player) extends OutgoingEvent
