import org.specs2.mutable._

import akka.actor._
import akka.testkit._
import play.api.libs.concurrent.Akka
import play.api.test._

import models._

class TestActor extends Actor {
  def receive = {
    case _ => Nil
  }
}

abstract class TestActorSystem extends TestKit(ActorSystem()) with After with ImplicitSender {
  val broadcast = TestProbe()
  val probe = TestProbe()

  val game = system.actorOf(Props(new Game(broadcast.ref)))

  def after = system.shutdown()
}

class GameSpec extends Specification {
  sequential

  "Game" should {

    "reacts to joining player by logging him in" in new TestActorSystem {
      game.tell(PlayerJoin("session-id"), probe.ref)
      probe.expectMsgPF() { case PlayerLogin(Player("session-id", _)) => true }
    }

  }

}
