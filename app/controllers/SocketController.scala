package controllers

import scala.concurrent.duration._
import akka.util.Timeout
import akka.actor._
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee._
import play.api.mvc._
import play.api.libs.json._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

import socketio._
import socketio.PacketTypes._

import models._

class EventDispatcher(controller: SocketIOController) extends Actor with ActorLogging {
  def receive = {
    case event : OutgoingEvent => {
      System.err.println("Outgoing event: " + event.toString)
      controller.broadcastEvent(Json.toJson(event).toString)
    }
  }

  implicit val eventWrites = new Writes[OutgoingEvent] {
    def writes(event: OutgoingEvent) = event match {
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
      case PlayerLeft(id) => Json.obj(
        "name" -> "player:left",
        "args" -> Json.arr(Json.obj(
          "id" -> id
        ))
      )
      case BoardSynced(board) => Json.obj(
        "name" -> "board:synced",
        "args" -> Json.arr(Json.obj(
          "imageURL" -> board.imageURL,
          "width" -> board.width,
          "height" -> board.height,
          "pieces" -> Json.toJson(
            board.positions.map { position =>
              Json.obj(
                "id" -> board.at(position).get.id,
                "x" -> position.x,
                "y" -> position.y
              )
            }
          )
        ))
      )
      case PiecePicked(player, pieceId) => Json.obj(
        "name" -> "piece:picked",
        "args" -> Json.arr(Json.obj(
          "player_id" -> player.id,
          "piece_id" -> pieceId
        ))
      )
      case PieceMoved(pieceId, position) => Json.obj(
        "name" -> "piece:moved",
        "args" -> Json.arr(Json.obj(
          "piece_id" -> pieceId,
          "position" -> Json.obj(
            "x" -> position.x,
            "y" -> position.y
          )
        ))
      )
      case _ => Json.obj()
    }
  }
}

object MySocketIOController extends SocketIOController {

  val clientTimeout = Timeout(10.seconds)

  val dispatcher = Akka.system.actorOf(Props(new EventDispatcher(this)))

  val game = Akka.system.actorOf(Props[Game], name = "game")

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

  override def handleConnectionSetup(sessionId: String, enumerator: Enumerator[String]):
    (Iteratee[String, Unit], Enumerator[String]) = {
    val iteratee = Iteratee.foreach[String] {
      socketData =>
      wsMap(sessionId) ! ProcessPacket(socketData)
    }.mapDone {
      _ => {
        println("all done quit.")
        game.tell(PlayerLeave(sessionId), dispatcher)
      }
    }
    wsMap(sessionId) ! EventOrNoop
    (iteratee, enumerator)
  }

  private

  def parseIncomingEvent(sessionId: String, data: String): Option[IncomingEvent] = {
    try {
      val json = Json.parse(data)
      val eventName = json \ "name"
      val eventArgs = Json.fromJson[List[JsValue]](json \ "args")
      (eventName, eventArgs) match {
        case (JsString("player:join"), _) => Some(PlayerJoin(sessionId))
        case (JsString("player:sync"), _) => Some(PlayersSync(sessionId))
        case (JsString("board:sync"), _) => Some(BoardSync(sessionId))
        case (JsString("piece:pickup"), JsSuccess(List(JsString(pieceId)), _)) => Some(PiecePickup(sessionId, pieceId))
        case (JsString("piece:move"), JsSuccess(List(JsObject(Seq(("id", JsString(pieceId)), ("x", JsNumber(x)), ("y", JsNumber(y))))), _)) => Some(PieceMove(sessionId, pieceId, x.toInt, y.toInt))
        case _ => None
      }
    }
    catch {
      case _ => None
    }
  }

}
