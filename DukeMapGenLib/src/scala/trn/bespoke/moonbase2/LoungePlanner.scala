package trn.bespoke.moonbase2

import scala.collection.mutable

/** Item that goes along the wall */
case class Item(name: String, length: Int)

case class ItemGroup(name: String, items: Seq[Item]){
  val length = items.map(_.length).sum
}

/**
  * Creates sectors to make a "hallway-lounge"
  */
object LoungePlanner {

  val Fill = "fill"
  val WI = Item("wallinsert", 768) // insert in wall, water fountain, cabinet, security monitor, etc
  val WP = Item("wallinsert+", 1024) // larger wall insert: two screens, or alcove (no cabinet door)
  val WL = Item("wallinsert-", 512) // just a decal

  val S = Item("space", 512) // space that normally goes on either end
  val T = Item("table", 512)
  val C2 = Item("2xchair", 512)
  val C3 = Item("3xchair", 768)
  val C4 = Item("4xchair", 1024)
  val C5 = Item("5xchair", 1280)
  val C6 = Item("6xchair", 1536)

  // TODO this goes in the middle of the floor.  I was thinking it would be cool if the walls were automatically made
  // blank next to it, but not sure of the best way to do that yet
  val I = Item("infokiosk", 1024) //it doesnt go on the wall; this is the clearance it needs

  // has one group of chairs.  The 1536 is a min size for 2 chairs
  // this can stretch up to 3072
  val G1 = Item("1xgroup", 1536) // TODO WARNING this is less than half of the G2 -- might need two different G1s (one is fixed at 4 chairs, or one has large then 4 chars)

  // two groups of chairs.  Min value of 3584 is for 4 chairs each
  // val G2 = Item("2xgroup", 3584)
  val Group2 = ItemGroup("2xgroup", Seq(C4, WI, C4)) // 2816

  // four groups of chairs
  val G4 = Item("4xgroup", 0)

  // group with centerpiece, the largest group type.  min size assumes 4-char groups and centerpiece is 512
  // TODO wait this is 12800
  val GC = Item("4xgroup", 6656)

  val SingleGroups = Seq(
    Seq(S, C2, S),
    Seq(S, C3, S),
    Seq(S, C4, S),
    Seq(S, C5, S),
    Seq(WI, C4, WI),
    Seq(WP, C4, WI),
    Seq(WP, C5, WI),
    Seq(WP, C6, WI)
  )

  def singleGroup(length: Int): Seq[Item] = {
    require(length >= G1.length)

    val groupAndLength = SingleGroups.map(seq => (seq, seq.map(_.length).sum))
    val (group, groupLength) = groupAndLength.filter{ case (_, i) => i < length }.maxBy{ case (_, i) => i}

    val (fill1, fill2) = fill(length - groupLength)
    Seq(Item(Fill, fill1)) ++ group ++ Seq(Item(Fill, fill2))
  }

  /** using as few groups as possible to come up with the length.  reminds me of binomial heap */
  def binomialGroup(length: Int): Seq[Item] = {
    require(length >= 1536) // dont want this to handle the stuff below one group

    var i = length
    val results = mutable.ArrayBuffer[Item]()
    while(i > GC.length){
      i -= GC.length
      results :+ GC
      if(i > WI.length){
        i -= WI.length
        results :+ WI
      }
    }

    if(i > Group2.length){
      i -= Group2.length
      results ++= Group2.items
      if(i > WI.length){
        i -= WI.length
        results :+ WI
      }
    }
    if(i > G1.length){
      results ++= singleGroup(i)
    }
    results
  }

  def fill(remainingLength: Int): (Int, Int) = {
    val f = remainingLength / 2
    (f, remainingLength - f) // - catch leftover 1
  }

  def addFill(items: Seq[Item], totalLength: Int): Seq[Item] = {
    val i = items.map(_.length).sum
    if(i >= totalLength){
      items
    }else{
      val (fill1, fill2) = fill(totalLength - i)
      Seq(Item(Fill, fill1)) ++ items ++ Seq(Item(Fill, fill2))
    }
  }

  /**
    *
    * @param length the width of the wall, in build units
    * @returns the items that make up the wall, from left to right from a POV facing the wall
    */
  def planWall(length: Int): Seq[Item] = {

    if(length < 512) {
      Seq(Item(Fill, length))
    } else if(length < 768) {
      addFill(Seq(WL), length)
    } else if(length < 1536) {
      // wall item
      addFill(Seq(WI), length)
    }else{
      binomialGroup(length)
    }
  }


  def main(args: Array[String]): Unit = {
    printSomeGroups()
    // printrow(planWall(400))
    // printrow(planWall(600))
    // printrow(planWall(1000))
    // printrow(planWall(2048))
    // printrow(planWall(4096))
  }

  def printrow(items: Seq[Item]): Unit = {
    val total = items.map(_.length).sum
    println(s"${total}\t\t${items.map(_.name).mkString(" | ")}")
  }

  def printSomeGroups(): Unit = {

    printrow(Seq(Item(Fill, 0)))
    printrow(Seq(S))
    printrow(Seq(WI))
    printrow(Seq(S, C2, S))
    printrow(Seq(S, C3, S))
    printrow(Seq(S, C4, S))
    printrow(Seq(S, C5, S))
    printrow(Seq(S, C6, S))
    printrow(Seq(WI, C4, WI))
    printrow(Seq(WI, C4, WP))
    printrow(Seq(WI, C5, WP))
    printrow(Seq(WI, C6, WP))

    // 3584
    println()
    printrow(Seq(S, C4, T, C4, S))
    printrow(Seq(S, C4, WI, C4, S))
    printrow(Seq(WI, C4, T, C4, WI))
    printrow(Seq(WI, C4, WI, C4, WI))
    printrow(Seq(S, C5, WI, C5, S))
    printrow(Seq(WI, C5, T, C5, WI))
    printrow(Seq(WI, C5, WI, C5, WI))
    printrow(Seq(S, C6, WI, C6, S))
    printrow(Seq(WI, C6, T, C6, WI))
    printrow(Seq(WI, C6, WI, C6, WI))

    println()
    printrow(Seq(S, C4, T, C4, T, C4, S))
    printrow(Seq(WI, C4, T, C4, T, C4, WI))
    printrow(Seq(WI, C4, WI, C4, WI, C4, WI))
    printrow(Seq(S, C5, T, C5, T, C5, S))

    println("\nFor the binomial thing (took spaces off the ends)") // TODO make sure entire sequence has a space at beginning...
    // TODO also need stuff between the groups
    printrow(Seq(C4, WI, C4)) // 2816
    printrow(Seq(C4, T, C4, WI, C4, T, C4)) // 5888
    printrow(Seq(C6, T, C6, WI, C6, T, C6)) // 5888
    // printrow(Seq(C4, T, C4, WI, C4, T, C4, Item("centerpiece", 1024), C4, T, C4, WI, C4, T, C4)) // 12800

  }
}
