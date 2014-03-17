package controllers

import scala.util.Random
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
      System.err.println("Outgoing broadcast event: " + event.toString)
      controller.broadcastEvent(Json.toJson(event).toString)
    }
  }

  implicit val serializer = new Serializer
}

class GameManager(broadcast: ActorRef) extends Actor with ActorLogging {
  val game = context.actorOf(Props(new Game(broadcast)), name = "game")

  def receive = {
    case BoardRequest => {
      game ! BoardChange(newBoard)
    }

    case event@_ => game.forward(event)
  }

  override def preStart = self ! BoardRequest

  private

  val images = List(
    Image("http://distilleryimage0.ak.instagram.com/da5da88ca91311e3b145120e7295bf20_8.jpg", "http://instagram.com/p/lZsC2anc1C"),
    Image("http://distilleryimage8.ak.instagram.com/3af3b0c2a91111e38d6c129f0cea75bc_8.jpg", "http://instagram.com/p/lZp8eKK35z"),
    Image("http://distilleryimage0.ak.instagram.com/0f69c3aca91511e3b2630e9c0aaa8964_8.jpg", "http://instagram.com/p/lZs_zHuaYP"),
    Image("http://distilleryimage4.ak.instagram.com/1c298576a90d11e3b4de12ff50c9f474_8.jpg", "http://instagram.com/p/lZme-zAm-B"),
    Image("http://distilleryimage6.ak.instagram.com/6191e200971711e3bad01215527ad906_8.jpg", "http://instagram.com/p/kewEAyOjTR"),
    Image("http://distilleryimage7.ak.instagram.com/57a2ac147ed211e3962a12bf16838833_8.jpg", "http://instagram.com/p/jPQOf6u6ez"),
    Image("http://distilleryimage3.ak.instagram.com/ccb35e56a6c111e38aca0e51ab9d14d3_8.jpg", "http://instagram.com/p/lSFbkQgr8N"),
    Image("http://distilleryimage3.ak.instagram.com/992f8acca6c211e38f0d1262a4bedf57_8.jpg", "http://instagram.com/p/lSGAzqFN5C")
  )

  val sizes = List((5, 5), (6, 6), (8, 8), (10, 10))

  def newBoard: Board = {
    val image = Random.shuffle(images).head
    val (width, height) = Random.shuffle(sizes).head
    val board = new Board(image, width, height)
    board.shuffle
    board
  }
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
      case _: Throwable => None
    }
  }

}
