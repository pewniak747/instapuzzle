package models

class Event

case class PlayerJoin(sessionId: String) extends Event

case class PlayerJoined(id: String, name: String) extends Event

case class PlayerLeave(sessionId: String) extends Event

case class PlayerLeft(id: String) extends Event

case class PlayersSync extends Event

case class PlayersSynced(players: Seq[Player]) extends Event

