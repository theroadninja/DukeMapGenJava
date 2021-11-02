package trn.bespoke.moonbase2

import trn.{BuildConstants, RandomX}

import scala.collection.mutable

/** Item that goes along the wall */
// TODO dedupe this with LoungePlanner.scala
// I had this as `Item` but then got "already defind" compile errors bc of lounge planner
case class Item3(name: String, length: Int)

object Item3 {

  def apply(name: String, length: Int): Item3 = {
    require(length > 0)
    new Item3(name, length)
  }

  def unapply(item: Item3): Option[(String, Int)] = Some((item.name, item.length))
}

/**
  * Lounge Planner 1 didnt work out; got way too complicated way to fast.  This one is much simpler at the cost of
  * being obvious with its patterns.
  *
  * 0 - 2048:           S
  * 2048 - 4608         S : CG : S
  * 3584+               S : CG : B | B : CG : S
  * 6912 - 12032        S : SUPERG : S
  * 11008+              S : B : SUPERG : B : S
  *
  * S = space or small group (min length is 0 by itself, but needs to be min 512 to give clearance from diagonal redwalls)
  * CG = C | CTC  (1024 to 3584)
  * B = bulkhead (min 2048, needs to clearance from diagonal redwalls)
  * SUPERG = C T C Wi C T C
  *
  * C = group of 2-6 chairs
  * T = table (exactly 512)
  * Wi = Wall insert (a subset of S that is not empty but fits into 768) -- NOT fans
  * S = space/small group/wall insert (0 to 2048)
  */
object LoungePlanner2 {

  val Space = "space"
  val Chars4 = "4xchair"
  val WallInsert = "wallinsert"
  val BulkHead = "bulkhead"

  // Wall Inserts
  val Window = "window" // min size 384
  val PowerCabinet = "powercabinet" // power-up cabinet, min size 512
  val Medkit = "medkit" // medkit cabinet, min size 768
  val Fountain = "fountain" // water fountain in little hole
  val SecurityScreen = "securityscreen" // security monitor screen for cameras
  val Fans = "fans"
  // val Fans2 = "2fans"
  // val Fans3 = "3fans"
  val SpaceSuits = "spacesuits" // alcove behind glass with 2 space suits
  val TwoScreens = "twoscreens"
  val PowerHole = "powerhole" // doorless cabinet (hole in wall) for a powerup
  val EDFDecal = "edfdecal" // giant EDF sign

  val MinSizes = Map(
    Window -> 384,
    PowerCabinet -> 512,
    Medkit -> 768,
    Fountain -> 640,
    SecurityScreen -> 640,
    Fans -> LoungeWallPrinter.MinFanLength, // two fans
    // Fans3 -> 1824,
    SpaceSuits -> 896,
    TwoScreens -> 1024,
    PowerHole -> 768,
    EDFDecal -> 1920,

    BulkHead -> LoungeWallPrinter.BulkheadMinLength,
  )

  val Unique = Set(Medkit, SecurityScreen, Fans, SpaceSuits, EDFDecal)

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

  val Chairs: Seq[Item] = Seq(C2, C3, C4, C5, C6)

  def planWall(length: Int, uniqueItemsTaken: Set[String], r: RandomX): (Seq[Item]) = {

    // TODO this strategy does not leave enough space in the corners!
    // TODO also there is a bug where things to off the edge of the wall

    if(length <= 2048){
      // S
      planSmall(r, None, Some(length), uniqueItemsTaken)
    }else if(length <= 4608){
      // S : CG : S
      val cg = planChairGroup(length - 1024)
      val used = lengthOf(cg)
      val left = (length - used) / 2
      planSmall(r, Some(512), Some(left), uniqueItemsTaken, Some(true)) ++ cg ++ planSmall(r, Some(512), Some(length - used - left), uniqueItemsTaken, Some(false))
    }else if(length < 6912){
      // S : CG : B | B : CG : S
      val minBulkhead = MinSizes(BulkHead)

      val cg = planChairGroup(length - 1024 - minBulkhead)
      val s = planSmall(r, Some(512), Some(length - lengthOf(cg) - minBulkhead), uniqueItemsTaken)
      require(lengthOf(s) <= 2048)
      val remainingLength = length - lengthOf(cg) - lengthOf(s)
      require(remainingLength > 0)
      val b = bulkhead(remainingLength)
      r.flipCoin{
        s ++ cg ++ b
      }{
        b ++ cg ++ s
      }
    }else if(length <= 12032){
      // S : SUPERG : S
      val sgc = planSuperGroup(r, length - 1024, uniqueItemsTaken)
      val space = length - lengthOf(sgc)
      val leftSpace = space / 2
      planSmall(r, Some(512), Some(leftSpace), uniqueItemsTaken, Some(true)) ++ sgc ++ planSmall(r, Some(512), Some(space - leftSpace), uniqueItemsTaken, Some(false))
    }else{
      // S : B : SUPERG : B : S
      val minBulkhead = 2 * MinSizes(BulkHead)
      val sgc = planSuperGroup(r, length - 1024 - minBulkhead, uniqueItemsTaken)
      val (sL, sR) = split(length - lengthOf(sgc) - minBulkhead)

      val spaceL = planSmall(r, None, Some(sL), uniqueItemsTaken)
      val spaceR = planSmall(r, None, Some(sR), uniqueItemsTaken)
      val (bL, bR) = split(length - lengthOf(sgc) - lengthOf(spaceL) - lengthOf(spaceR))
      spaceL ++ bulkhead(bL) ++ sgc ++ bulkhead(bR) ++ spaceR
    }
  }


  /**
    * Divides an Int into two halves, ensuring there are no off by 1 errors due to odd numbers.
    * i.e. f(i) => (a,b) where i=a+b and (i=2a or i=2b)
    *
    * @param i
    * @return (a,b)  where a and b are each half of i
    */
  def split(i: Int): (Int, Int) = {
    val a = i / 2
    (a, i - a) // - catch leftover 1
  }

  private def lengthOf(items: Seq[Item]): Int = items.map(_.length).sum

  def bulkhead(length: Int): Seq[Item] = {
    require(length > 0, s"invalid bulkhead length: ${length}")
    Seq(Item(BulkHead, length))
  }

  /**
    * SUPERG = C T C Wi C T C
    *
    * min length = 5888    ( C4=1024 T=512 C4 Wi=768 C4 T C4)
    * for simplicity make sure the wall insert has at least 768 and give it any extra
    *
    * max length somewhere around 7936 (for C6 and Wi=768) but could be a bit bigger
    * (the 12032 comes from adding a 2048-sized S on either end)
    */
  def planSuperGroup(r: RandomX, length: Int, uniqueItemsTaken: Set[String]): Seq[Item] = {
    val chairGroupLength = (length - (T.length * 2) - 768) / 4
    val cg = Chairs.filter(_.length <= chairGroupLength).last
    val wi = length - (cg.length * 4) - (T.length * 2)
    Seq(cg, T, cg) ++ planSmall(r, None, Some(wi), uniqueItemsTaken) ++ Seq(cg, T, cg)
  }

  /**
    * CG = C | CTC  (1024 to 3584)
    */
  def planChairGroup(length: Int): Seq[Item] = {
    require(length >= 1024)
    // C6 is 1536  (same as C2 T C2)
    // C3 T C3 == 2048
    // C4 T C4 == 2560 long
    if(length < 1536){
      // C
      Seq(Chairs.filter(_.length <= length).last)
    }else{
      // C T C
      val c: Item = Chairs.filter(_.length * 2 + T.length <= length).last
      Seq(c, T, c)
    }
  }

  /**
    * The smallest wall lengths: 0 to 2048
    * @param lengthOpt how much space to fill, None to pick any wall insert randomly regardless of size
    * @param uniqueItemsTaken set of item names that can't be used again
    * @param side:  used when more then one space is calculated at once.  unique items are divided into groups that
    *            can only be used if side=True, and ones that can only be used if side=False.  Just a way for me to
    *            be lazy in the function that calls this one.
    * @return
    */
  def planSmall(r: RandomX, minLengthOpt: Option[Int], lengthOpt: Option[Int], uniqueItemsTaken: Set[String], side: Option[Boolean] = None): Seq[Item] = {
    val length = lengthOpt.getOrElse(BuildConstants.MAX_X * 2)
    require(length >= 0)
    minLengthOpt.foreach ( i => require(i <= length))
    // TODO use this somewhere else: def fancount(length: Int) = Math.max(3, (length - 288) / 512) // each fan is 512, can have 1 to 3 fans, and need 144 clearance on each side b/c it sticks out of the wall by 128
    val smallerOptions = Seq(Window, PowerCabinet, Medkit, Fountain, SecurityScreen)

    def uniqueOk(item: String): Boolean = {
      ! (Unique.contains(item) && (uniqueItemsTaken.contains(item) || side.getOrElse(false)))
    }

    if(length < 384){
      // just empty space
      Seq(Item(Space, length))
    } else if(length <= 768){
      val options = smallerOptions.filter(s => MinSizes(s) <= length).filter(uniqueOk)
      require(options.nonEmpty)
      // TODO need to weight the choices so that some items are rare (appear very infrequently)
      Seq(Item(r.randomElement(options), length))
    }else{
      val options = (smallerOptions ++ Seq(SpaceSuits, TwoScreens, PowerHole, EDFDecal, Fans))
        .filter(s => MinSizes(s) <= length)
        .filter(uniqueOk)
      require(options.nonEmpty)
      // TODO need to weight the choices so that some items are rare (appear very infrequently)
      Seq(Item(r.randomElement(options), length))
    }
  }

  def planWallOld(length: Long): Seq[Item] = {
    //return Seq(Item(Space, 512), Item(Window, 384), Item(Space, 512))
    // return Seq(Item(BulkHead, 2048))
    // return Seq(Item(Fountain, 768))
    // return Seq(Item(Fans, 2048))
    return Seq(Item(SpaceSuits, 1024))



    val results = mutable.ArrayBuffer[Item]()
    // TODO

    var length2 = length
    length2 -= 512 // this is for the space at the end

    // TEMPORARY
    if(length2 > 768 + 512){
      results += WI
      length2 -= 768
    }
    while(length2 > C5.length + 512){
      results += S
      results += C5
      length2 -= 512 + C5.length
    }

    while(length2 > 1024 + 512){
      results += S
      results += C4
      length2 -= (1024 + 512)
    }

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

  def main(args: Array[String]): Unit = {
    val r = RandomX()
    // (0 to 2048).foreach { i =>
    //   println(planSmall(r, Some(i), Set.empty))
    // }
    (1024 to 3584).foreach { i =>
      println(planChairGroup(i))
    }
  }
}
