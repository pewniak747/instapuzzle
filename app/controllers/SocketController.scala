package controllers

import scala.concurrent.duration._
import akka.util.Timeout
import play.api.mvc._
import play.api.libs.json._

import socketio._
import socketio.PacketTypes._

class Event

case object PlayerJoin extends Event

case class PlayerJoined(id: String, name: String) extends Event

case class PlayerLeft(id: String) extends Event

object MySocketIOController extends SocketIOController {

  val clientTimeout = Timeout(10.seconds)

  def processMessage(sessionId: String, packet: Packet) {
    parseIncomingEvent(packet.data).map {
      case PlayerJoin => broadcastEvent(PlayerJoined(sessionId, "tom"))
      case _ => broadcastEvent(packet.data)
    }
  }

  override def initSession = Action { implicit request =>
    val sessionId = java.util.UUID.randomUUID().toString
    System.err.println("Strating new session: " + sessionId)
    val t = clientTimeout.duration.toSeconds.toString
    Ok(sessionId + ":" + t + ":" + t +":websocket")
  }

  private

  def parseIncomingEvent(data: String): Option[Event] = {
    try {
      val json = Json.parse(data)
      val eventName = json \ "name"
      val eventArgs = (json \ "args")
      (eventName, eventArgs) match {
        case (JsString("player:join"), _) => Some(PlayerJoin)
        case _ => None
      }
    }
    catch {
      case _ => None
    }
  }

  def broadcastEvent(event: Event) {
    broadcastEvent(Json.toJson(event).toString)
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
      case _ => Json.obj()
    }
  }

}
