package controllers

import scala.concurrent.duration._
import scala.util.Try
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
      System.err.println("Outgoing broadcast event: " + event.toString)
      controller.broadcastEvent(Json.toJson(event).toString)
    }
  }

  implicit val serializer = new Serializer
}

object MySocketIOController extends SocketIOController {

  val clientTimeout = Timeout(10.seconds)

  val dispatcher = Akka.system.actorOf(Props(new EventDispatcher(this)))

  val gameManager = Akka.system.actorOf(Props(new GameManager(dispatcher)))

  def processMessage(sessionId: String, packet: Packet) {
    parseIncomingEvent(sessionId, packet.data).map { event =>
      System.err.println("Incoming event: " + event.toString)
      gameManager.tell(event, wsMap(sessionId))
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
    }.map {
      _ => {
        println("all done quit.")
        gameManager ! PlayerLeave(sessionId)
      }
    }
    wsMap(sessionId) ! EventOrNoop
    (iteratee, enumerator)
  }

  override def wsHandler(sessionId: String) = WebSocket.using[String] {
    implicit request =>
      if (wsMap contains sessionId) {
        handleConnectionFailure(Json.stringify(Json.toJson(Map("error" -> "Invalid Session ID"))))
      } else {
        println("creating new websocket actor")
        val (in, channel) = Concurrent.broadcast[String]
        val wsActor = Akka.system.actorOf(Props(new WebsocketActor(channel, processMessage, wsMap, wsRevMap, clientTimeout)))
        wsMap    += (sessionId -> wsActor)
        wsRevMap += (wsActor -> sessionId)
        handleConnectionSetup(sessionId, in)
      }
  }

  private

  def parseIncomingEvent(sessionId: String, data: String): Try[IncomingEvent] = Try {
    val json = Json.parse(data)
    val eventName = json \ "name"
    val eventArgs = Json.fromJson[List[JsValue]](json \ "args")
    (eventName, eventArgs) match {
      case (JsString("player:join"), _) => PlayerJoin(sessionId)
      case (JsString("player:sync"), _) => PlayersSync(sessionId)
      case (JsString("board:sync"), _) => BoardSync(sessionId)
      case (JsString("piece:pickup"), JsSuccess(List(JsString(pieceId)), _)) => PiecePickup(sessionId, pieceId)
      case (JsString("piece:move"), JsSuccess(List(JsObject(Seq(("id", JsString(pieceId)), ("x", JsNumber(x)), ("y", JsNumber(y))))), _)) => PieceMove(sessionId, pieceId, x.toInt, y.toInt)
    }
  }

}
