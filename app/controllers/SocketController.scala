package controllers

import scala.concurrent.duration._
import akka.util.Timeout
import play.api.mvc._

import socketio._
import socketio.PacketTypes._

object MySocketIOController extends SocketIOController {

  val clientTimeout = Timeout(10.seconds)

  def processMessage(sessionId: String, packet: Packet) {
    enqueueJsonMsg(sessionId, """{"hello":"hai"}""") 
  }

  override def initSession = Action { implicit request =>
    val sessionId = java.util.UUID.randomUUID().toString
    System.err.println("Strating new session: " + sessionId)
    val t = clientTimeout.duration.toSeconds.toString
    Ok(sessionId + ":" + t + ":" + t +":websocket")
  }

}
