package trn.bespoke.moonbase2

import scala.collection.mutable

/** Item that goes along the wall */
case class Item(name: String, length: Int)

/** Lounge Planner 1 didnt work out */
object LoungePlanner2 {

  val Space = "space"
  val Chars4 = "4xchair"
  val WallInsert = "wallinsert"

  val WI = Item("wallinsert", 768) // insert in wall, water fountain, cabinet, security monitor, etc
  val WP = Item("wallinsert+", 1024) // larger wall insert: two screens, or alcove (no cabinet door)
  val WL = Item("wallinsert-", 512) // just a decal

  val S = Item(Space, 512) // space that normally goes on either end
  val T = Item("table", 512)
  val C2 = Item("2xchair", 512)
  val C3 = Item("3xchair", 768)
  val C4 = Item(Chars4, 1024)
  val C5 = Item("5xchair", 1280)
  val C6 = Item("6xchair", 1536)

  def planWall(length: Long): Seq[Item] = {
    val results = mutable.ArrayBuffer[Item]()
    // TODO

    var length2 = length
    while(length2 > 1024){
      if(length2 > 512){
        results += S
        length2 -= 512
      }
      if(length2 > 512){
        results += C2
        length2 -= 512
      }
    }

    results += S

    results
  }
}
