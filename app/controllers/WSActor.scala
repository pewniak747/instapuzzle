package controllers

import scala.concurrent.duration._
import scala.util.matching.Regex

import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.api.Play.current

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import socketio._
import socketio.PacketTypes._

import models._

class WebsocketActor(channel: Concurrent.Channel[String], processMessage: (String, Packet) => Unit, wsMap: collection.mutable.Map[String, ActorRef], wsRevMap: collection.mutable.Map[ActorRef, String], clientTimeout: Timeout) extends WSActor(channel: Concurrent.Channel[String], processMessage: (String, Packet) => Unit, wsMap: collection.mutable.Map[String, ActorRef], wsRevMap: collection.mutable.Map[ActorRef, String], clientTimeout: Timeout) {

  implicit val serializer = new Serializer

  override def enqueue: Receive = {
    case Enqueue(x) => channel.push(x)
    case event : OutgoingEvent => {
      System.err.println("Outgoing sender event: " + event.toString)
      val payload = Json.toJson(event).toString
      self ! Enqueue(Parser.encodePacket(Packet(packetType = EVENT, endpoint = "", data = payload)))
    }
  }
}
