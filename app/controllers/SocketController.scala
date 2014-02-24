package controllers

import scala.concurrent.duration._
import akka.util.Timeout
import akka.actor._
import play.api.libs.concurrent.Akka
import play.api.mvc._
import play.api.libs.json._
import play.api.Play.current

import socketio._
import socketio.PacketTypes._

import models.GameActor
import models.Player

class Event

case class PlayerJoin(sessionId: String) extends Event

case class PlayerJoined(id: String, name: String) extends Event

case class PlayerLeave(sessionId: String) extends Event

case class PlayerLeft(id: String) extends Event

case class PlayersSync extends Event

case class PlayersSynced(players: Seq[Player]) extends Event

class EventDispatcher(controller: SocketIOController) extends Actor with ActorLogging {
  def receive = {
    case event : Event => {
      System.err.println("Outgoing event: " + event.toString)
      controller.broadcastEvent(Json.toJson(event).toString)
    }
  }

  implicit val eventWrites = new Writes[Event] {
    def writes(event: Event) = event match {
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
      case _ => Json.obj()
    }
  }
}

object MySocketIOController extends SocketIOController {

  val clientTimeout = Timeout(10.seconds)

  val dispatcher = Akka.system.actorOf(Props(new EventDispatcher(this)))

  val game = Akka.system.actorOf(Props[GameActor], name = "game")

  def processMessage(sessionId: String, packet: Packet) {
    parseIncomingEvent(sessionId, packet.data).map { event =>
      System.err.println("Incoming event: " + event.toString)
      game.tell(event, dispatcher)
    }
  }

  override def initSession = Action { implicit request =>
    val sessionId = java.util.UUID.randomUUID().toString
    System.err.println("Strating new session: " + sessionId)
    val t = clientTimeout.duration.toSeconds.toString
    Ok(sessionId + ":" + t + ":" + t +":websocket")
  }

  private

  def parseIncomingEvent(sessionId: String, data: String): Option[Event] = {
    try {
      val json = Json.parse(data)
      val eventName = json \ "name"
      val eventArgs = (json \ "args")
      (eventName, eventArgs) match {
        case (JsString("player:join"), _) => Some(PlayerJoin(sessionId))
        case (JsString("player:sync"), _) => Some(PlayersSync())
        case _ => None
      }
    }
    catch {
      case _ => None
    }
  }

}
