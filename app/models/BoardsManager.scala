package models

import scala.util.Random
import akka.actor._

class BoardsManager extends Actor with ActorLogging {

  def receive = {
    case BoardRequest => {
      val image = Random.shuffle(images).head
      val dimensions = Random.shuffle(sizes).head
      sender ! BoardResponse(image, dimensions)
    }
  }

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

}
