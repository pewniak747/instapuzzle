package models

import akka.actor._

import scala.collection.mutable

import controllers._

case class Player(id: String, name: String)

class Game extends Actor with ActorLogging {

  var playersMap:  mutable.Map[String, Player] = mutable.Map.empty

  def receive = {
    case PlayerJoin(sessionId) => {
      val id = sessionId
      val name = "random name"
      playersMap.put(sessionId, Player(id, name))
      sender ! PlayerJoined(id, name)
    }
    case PlayerLeave(sessionId) => {
      playersMap.remove(sessionId).map { player =>
        sender ! PlayerLeft(player.id)
      }
    }
    case PlayersSync() => {
      sender ! PlayersSynced(playersMap.values.toList)
    }
    case _ => Nil
  }

}
