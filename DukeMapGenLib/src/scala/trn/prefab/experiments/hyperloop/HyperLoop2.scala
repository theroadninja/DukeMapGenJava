package trn.prefab.experiments.hyperloop

import trn.duke.TextureList
import trn.prefab.experiments.ExpUtil
import trn.prefab.experiments.hyperloop.EdgeIds.{OuterEdgeConn, InnerEdgeConn}
import trn.prefab.experiments.hyperloop.Loop2Plan.{Blank, ForceField, LightWeapon}
import trn.prefab.experiments.hyperloop.RingLayout.{DIAG, AXIS}
import trn.prefab.experiments.hyperloop.RingPrinter2.{OuterRing, MiddleRing, InnerRing, AllRings}
import trn.{Sprite, HardcodedConfig, RandomX, ScalaMapLoader}
import trn.prefab.{MapWriter, DukeConfig, GameConfig, SectorGroup, RedwallConnector, PastedSectorGroup, SpriteLogicException, Marker}
import trn.prefab.experiments.hyperloop.Item._

import scala.collection.mutable

/** rough sense of how difficult the enemies should be in an area */
object ThreatLevel {
  val Low = 0
  val High = 1
}

// case class Shape(ringIndex: Int, angleType: Int)

/**
  *
  * @param inner
  * @param outer
  * @param generalThreat rough idea of threat level for that section -- sections can override though
  */
case class Loop2Plan(
  inner: Seq[String],
  outer: Seq[String],
  generalThreat: Seq[Int],
){
  require(inner.size == outer.size && outer.size == generalThreat.size)

  def isForceField(index: Int): Boolean = {
    val a = inner(index) == ForceField
    val b = inner(index) == ForceField
    require(a == b)
    return a
  }

  def validate(): Loop2Plan = {
    val all = inner ++ outer
    require(all.filter(_ == Loop2Plan.Start).size == 1)
    require(all.filter(_ == Loop2Plan.End).size == 1)
    this
  }
}

class Loop2Planner(random: RandomX, size: Int = 16){

  def testPlan: Loop2Plan = {
    require(size == 16)
    val inner = "_L_____F_Z____F_"
    val outer = "*S_k___F__K___F_"
    val threat = Seq(0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0)

    require(inner.size == size)
    require(outer.size == size)
    Loop2Plan(
      inner.map(s => s.toString),
      outer.map(s => s.toString),
      threat,
    ).validate()
  }

  def findEmpty(ring: mutable.Map[Int, String], range: Seq[Int]): Int = {
    random.randomElement((0 until size).filter(range.contains).filterNot(ring.contains))
  }

  def randomPlan(): Loop2Plan = {
    return testPlan
    require(size == 16)
    // TODO add validation, like ensuring there is exactly one start, one end, etc

    val secureZoneStart = 6 + random.scalaRandom.nextInt(2) // end value is exclusive
    val secureZoneEnd =  13 + random.scalaRandom.nextInt(2) // 13-15
    val secureZone = (secureZoneStart to secureZoneEnd)
    val clearZone = (0 until size).filterNot(secureZone.contains)

    val inner = mutable.Map[Int, String]()
    val outer = mutable.Map[Int, String]()

    inner.put(secureZone.head, ForceField)
    outer.put(secureZone.head, ForceField)
    inner.put(secureZone.last, ForceField)
    outer.put(secureZone.last, ForceField)

    val startLoc = findEmpty(outer, clearZone)
    outer.put(startLoc, Loop2Plan.Start)

    val lightWeaponRange = clearZone.filter(i => Math.abs(i - startLoc) < 2)
    inner.put(findEmpty(inner, lightWeaponRange), Loop2Plan.LightWeapon) // TODO outer or inner

    outer.put(findEmpty(outer, clearZone), Loop2Plan.Key1)
    outer.put(findEmpty(outer, secureZone), Loop2Plan.Key2)

    inner.put(findEmpty(inner, (0 until size)), Loop2Plan.End)

    Loop2Plan(
      (0 until size).map(i => inner.getOrElse(i, Blank)),
      (0 until size).map(i => outer.getOrElse(i, Blank)),
      (0 until size).map { i =>
        if(secureZone.contains(i)){
          ThreatLevel.High
        }else{
          ThreatLevel.Low
        }
      }
    ).validate()
  }

}

object Loop2Plan {

  val Test = "*" // for testing

  val Blank = "_" // val Blank = "_"

  /** like blank, except that it can have weapons, powerups, enemies
    */
  // val Random = "R"
  val Random = "R"

  val Decor = "D" // just using as a subsection for random

  /** outer section containing player start */
  val Start = "S"

  /** inner section, or set piece, containing end (always requires the second key to access) */
  val End = "Z"

  // TODO ? val Blocked = "*" // for things like end having two connections

  /** first key */
  val Key1 = "k"

  /** second key */
  val Key2 = "K"

  /** set piece that blocks the passage; force field or "blast doors" */
  val ForceField = "F"

  /** 80% shotgun, 10% chaingun, 10% pipe bombs */
  val LightWeapon = "L"

  // TODO tripmine and shrink ray to have lower probability
  val Weapon = "W" // "H" for "heavy weapon" ?

  /** currently includes ammo */
  val Powerup = "P"

  /** enemy section - section dedicated to enemies.  NOT used for enemies that spawn in the passage */
  val Enemy = "E"

}

/** for set piees, a single pasted sectorgroup may be referenced by multiple pasted sections */
case class PastedSection(
  psg: PastedSectorGroup,
  inner: Option[RedwallConnector],
  antiClockwise: RedwallConnector,
  clockwise: RedwallConnector,
  outer: Option[RedwallConnector],
) {

  lazy val switchesRequested: Seq[Sprite] = psg.allSpritesInPsg.filter(s => Marker.isMarker(s, Marker.Lotags.SWITCH_REQUESTED))
}

object PastedSection {

  def getConn(psg: PastedSectorGroup, connId: Int): Option[RedwallConnector] = {
    psg.redwallConnectors.find(c => c.getConnectorId == connId)
  }
  def apply(psg: PastedSectorGroup): PastedSection = PastedSection(
    psg,
    getConn(psg, InnerEdgeConn),
    getConn(psg, EdgeIds.AntiClockwiseEdge).get,
    getConn(psg, EdgeIds.ClockwiseEdge).get,
    getConn(psg, OuterEdgeConn),
  )

  def setPiece(psg: PastedSectorGroup): Seq[PastedSection] = {
    val inner = PastedSection(
      psg,
      None,
      getConn(psg, EdgeIds.AntiClockwiseEdge).get,
      getConn(psg, EdgeIds.ClockwiseEdge).get,
      None
    )
    val mid = PastedSection(
      psg,
      None,
      getConn(psg, EdgeIds.SetPieceMidAnticlockwise).get,
      getConn(psg, EdgeIds.SetPieceMidClockwise).get,
      None,
    )
    val outer = PastedSection(
      psg,
      None,
      getConn(psg, EdgeIds.SetPieceOuterAnticlockwise).get,
      getConn(psg, EdgeIds.SetPieceOuterClockwise).get,
      None,
    )
    Seq(inner, mid, outer)
  }
}

object RingPrinter2 {
  val InnerRing = RingIndex.InnerRing
  val MiddleRing = RingIndex.MiddleRing
  val OuterRing = RingIndex.OuterRing
  val AllRings = Seq(InnerRing, MiddleRing, OuterRing)
}

class RingPrinter2(writer: MapWriter, layout: RingLayout, columnCount: Int) {
  require(columnCount % layout.anglesPer360 == 0)

  val loopCount = columnCount / layout.anglesPer360  // how many times it goes around
  val rings = Map(
    InnerRing -> mutable.Map[Int, PastedSection](),
    MiddleRing -> mutable.Map[Int, PastedSection](),
    OuterRing -> mutable.Map[Int, PastedSection](),
  )

  def paste(ringIndex: Int, colIndex: Int, sgToPaste: SectorGroup): PastedSection = {
    require(colIndex < columnCount)
    require(ringIndex == InnerRing || ringIndex == MiddleRing || ringIndex == OuterRing)

    val heading = RingLayout.indexToHeading(colIndex)
    val sg = RingLayout.rotationToHeading(heading).rotate(sgToPaste)

    ringIndex match {
      case MiddleRing => {
        if (rings(MiddleRing).contains(colIndex)) {
          throw new RuntimeException(s"there is already a middle ring piece at index ${colIndex}")
        }
        val loc = layout.midRingAnchors(heading).withZ(0)
        val psg = writer.pasteSectorGroupAt(sg, loc, true)
        val ps = PastedSection(psg)
        rings(MiddleRing).put(colIndex, ps)
        ps
      }
      case InnerRing | OuterRing => {
        if(rings(ringIndex).contains(colIndex)){
          throw new RuntimeException(s"there is already a pasted group at ring ${ringIndex}, column ${colIndex}")
        }
        val midPsg = rings(MiddleRing).get(colIndex).getOrElse(throw new Exception(s"cannot paste innerSg at index ${colIndex}, there is no middle sg to latch onto"))
        val midConn = ringIndex match {
          case InnerRing => midPsg.inner.get
          case OuterRing => midPsg.outer.get
        }
        val newSgConn = ringIndex match {
          case InnerRing => sg.getRedwallConnector(OuterEdgeConn)
          case OuterRing => sg.getRedwallConnector(InnerEdgeConn)
        }
        val ps = PastedSection(writer.pasteAndLink(midConn, sg, newSgConn, Seq.empty))
        rings(ringIndex).put(colIndex, ps)

        // touchplate
        if(ps.switchesRequested.size > 0){
          require(ps.switchesRequested.size == 1, "cannot handle more than 1 switch requested")
          addTouchplate(MiddleRing, colIndex, ps.switchesRequested.head.getHiTag())
        }
        ps
      }
    }
  }

  /**
    * paste a single sector group that fills 1 column
    */
  def pasteSetPiece(colIndex: Int, sgToPaste: SectorGroup): Seq[PastedSection] = {
    require(colIndex < columnCount)
    AllRings.foreach{ r =>
      if(rings(r).contains(colIndex)){
        throw new RuntimeException(s"cannot paste set piece.  there is already a pasted group at ring=${r} col=${colIndex}")
      }
    }

    val heading = RingLayout.indexToHeading(colIndex)
    val sg = RingLayout.rotationToHeading(heading).rotate(sgToPaste)
    val loc = layout.midRingAnchors(heading).withZ(0)
    val psg = writer.pasteSectorGroupAt(sg, loc, true)

    val pasted = PastedSection.setPiece(psg)
    rings(InnerRing).put(colIndex, pasted(0))
    rings(MiddleRing).put(colIndex, pasted(1))
    rings(OuterRing).put(colIndex, pasted(2))
    pasted
  }

  def tryPaste(ringIndex: Int, colIndex: Int, sg: SectorGroup): Option[PastedSection] = if(rings(ringIndex).contains(colIndex)) {
    None
  }else{
    Some(paste(ringIndex, colIndex, sg))
  }

  def tryPasteMid(colIndex: Int, midSg: SectorGroup): Option[PastedSection] = tryPaste(MiddleRing, colIndex, midSg)

  private def autoLinkRing(ring: mutable.Map[Int, PastedSection]): Unit = {
    val pairs: Seq[Seq[Option[PastedSection]]] = (0 until columnCount).map(ring.get).sliding(2).toSeq
    val pairs2 = pairs ++ Seq(Seq(ring.get(columnCount - 1), ring.get(0)))  // add first<->last
    pairs2.foreach { pair =>
      require(pair.size == 2)
      val a = pair(0)
      val b = pair(1)
      if(a.isDefined && b.isDefined){
        writer.autoLink(a.get.psg, b.get.psg)
      }
    }
  }

  def autoLink(): Unit = {
    Seq(InnerRing, MiddleRing, OuterRing).foreach(ringIndex => autoLinkRing(rings(ringIndex)))
  }

  /**
    * Adds a touchplate to a middle ring sector group
    *
    * @param psg   must be a mid group
    * @param lotag the lotag of the touchplate
    */
  def addTouchplate(ringIndex: Int, columnIndex: Int, lotag: Int): Unit = {
    val psg = rings(ringIndex)(columnIndex).psg
    val loc = psg.boundingBox.center
    val marker = psg.allSpritesInPsg.find(s => Marker.isMarker(s, Marker.Lotags.ALGO_GENERIC)).getOrElse(throw new Exception("missing marker for touchplate"))
    val touch = SpriteFactory.touchplate(loc.withZ(0), marker.getSectorId, lotag)
    psg.map.addSprite(touch)
  }


}

object HyperLoop2 {
  val Filename = "loop2.map"

  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)

    val random = new RandomX()

    val plan = new Loop2Planner(random).randomPlan()
    println(plan)

    val palette = ScalaMapLoader.loadPalette(HardcodedConfig.EDUKE32PATH + Filename, Some(gameCfg))
    val writer = MapWriter(gameCfg)
    try {
      renderPlan(gameCfg, random, writer, plan, new Loop2Palette(gameCfg, random, palette))
    } catch {
      case e: SpriteLogicException => {
        e.printStackTrace()
        ExpUtil.finishAndWrite(writer, removeMarkers=false)
      }
    }
  }

  def allSwitchRequests(psg: PastedSectorGroup): Seq[Int] = {
    psg.allSpritesInPsg.filter(s => Marker.isMarker(s, Marker.Lotags.SWITCH_REQUESTED)).map(_.getHiTag)
  }

  /**
    * takes two force fields and links them (so they use the same channel, and get opened simultaneously)
    * @param psgA
    * @param psgB
    */
  def linkForceFields(gameCfg: GameConfig, psgA: PastedSectorGroup, psgB: PastedSectorGroup): Unit = {
    val channelA = allSwitchRequests(psgA).head
    val channelB = allSwitchRequests(psgB).head
    // val tagMap = Map(channelB -> channelA)
    psgB.allSpritesInPsg.foreach { sprite =>
      // doesk work; we dont have the full map: gameCfg.updateUniqueTagInPlace(sprite, tagMap)
      if(sprite.getTex == TextureList.Switches.ACCESS_SWITCH_2) {
        sprite.setLotag(channelA)
      }
    }
    psgB.getAllWallViews.map(_.getWallId).foreach { wallId =>
      val wall = psgB.map.getWall(wallId)
      // doesk work; we dont have the full map: gameCfg.updateUniqueTagInPlace(wall, tagMap)
      if(wall.getMaskTex == TextureList.ForceFields.W_FORCEFIELD) {
        wall.setLotag(channelA)
      }
    }
  }

  def renderPlan(gameCfg: GameConfig, random: RandomX, writer: MapWriter, plan: Loop2Plan, loop2Palette: Loop2Palette): Unit = {
    val sectionCount = 16
    val ringLayout = RingLayout.fromHyperLoopPalette(loop2Palette)
    val ringPrinter = new RingPrinter2(writer, ringLayout, sectionCount)

    val key1 = Item.RedKey // for force fields
    val key2 = Item.YellowKey

    // Force Fields
    val pastedForceFields = (0 until sectionCount).filter(i => plan.isForceField(i)).map { index =>
      val angleType = RingLayout.indexToAngleType(index)
      val sg = Loop2Palette.makeRed(gameCfg, loop2Palette.getForceField(angleType))
      val pasted = ringPrinter.pasteSetPiece(index, sg)
      pasted(1) // return the middle one
    }
    linkForceFields(gameCfg, pastedForceFields(0).psg, pastedForceFields(1).psg)

    (0 until sectionCount).foreach { index =>
      val angleType = RingLayout.indexToAngleType(index)
      ringPrinter.tryPasteMid(index, loop2Palette.getMidBlank(angleType))
    }

    val picker = new SectionPicker(random, loop2Palette, key1, key2)

    (0 until sectionCount).foreach { index =>
      val angleType = RingLayout.indexToAngleType(index)
      val threat = plan.generalThreat(index)
      if(!ringPrinter.rings(InnerRing).contains(index)){
        val sg = picker.getSection(plan.inner(index), InnerRing, angleType, threat).get
        ringPrinter.tryPaste(InnerRing, index, sg)
      }
      if (!ringPrinter.rings(OuterRing).contains(index)) {
        val sg = picker.getSection(plan.outer(index), OuterRing, angleType, threat).get
        ringPrinter.tryPaste(OuterRing, index, sg)
      }
    }

    ringPrinter.autoLink()
    ExpUtil.finishAndWrite(writer)
  }

}

class SectionPicker(random: RandomX, palette: Loop2Palette, key1: Item, key2: Item) {

  val PowerUps = Seq[Item](
    Medkit, Armor, Steroids, Nightvision, HoloDuke, AtomicHealth, Jetpack
    // skipping scuba

  )

  val Weapons = Seq[Item](
    Shotgun,
    Chaingun,
    Rpg,
    FreezeRay,
    ShrinkRay,
    PipeBomb,
    Devastator,
  )

  val LightWeapons = Seq[Item](Shotgun, Chaingun)

  // TODO
  def getSection(sectionType: String, ringIndex: Int, angleType: Int, threat: Int): Option[SectorGroup] = {
    sectionType match {
      case Loop2Plan.Test => Some(palette.getTest(ringIndex, angleType))
      // case Loop2Plan.Blank => Some(getBlank(ringIndex, angleType))
      case Loop2Plan.Blank | Loop2Plan.Random => {
        // Some(getRandom(ringIndex, angleType))
        getSection(
          random.randomElement(Seq(Loop2Plan.Decor, Loop2Plan.Enemy, Loop2Plan.Weapon)), ringIndex, angleType, threat
        )
      }
      case Loop2Plan.Decor => Some(getDecor(ringIndex, angleType))
      case Loop2Plan.End => Some(getEnd(ringIndex, angleType, key2.pal))
      case Loop2Plan.Start => Some(getStart(ringIndex, angleType))
      case Loop2Plan.ForceField => ??? // this shouldn't be called
      case Loop2Plan.Key1 => {
        require(ringIndex == OuterRing, "inner keys not implemented yet") // TODO implement
        Some(palette.getFanOuter(angleType, key1))
      }
      case Loop2Plan.Key2 => {
        require(ringIndex == OuterRing, "inner keys not implemented yet") // TODO implement
        Some(palette.getFanOuter(angleType, key2))
      }
      case Loop2Plan.Enemy => Some(getEnemy(ringIndex, angleType, threat))
      case Loop2Plan.LightWeapon => getWeapon(ringIndex, angleType, random.randomElement(LightWeapons))
      case Loop2Plan.Weapon => getWeapon(ringIndex, angleType, random.randomElement(Weapons))
      case Loop2Plan.Powerup => getPowerUp(ringIndex, angleType, random.randomElement(PowerUps))
      case s: String => {
        println(s"ERROR ${s}")
        None
      }
    }
  }

  def getWeapon(ringIndex: Int, angleType: Int, weapon: Item): Option[SectorGroup] = {
    ringIndex match {
      case InnerRing => {
        Some(palette.armoryInnerSection(ringIndex, angleType).withItem(weapon.tex, weapon.pal))
      }
      case OuterRing => Some(palette.getWeaponOuter(angleType, weapon))
    }
  }

  def getPowerUp(ringIndex: Int, angleType: Int, weapon: Item): Option[SectorGroup] = {
    ringIndex match {
      case InnerRing => Some(palette.suppliesInnerSection(ringIndex, angleType).withItem(weapon.tex, weapon.pal))
      case OuterRing => Some(palette.getWeaponOuter(angleType, weapon)) // TODO better places
    }
  }

  def getRandom(ringIndex: Int, angleType: Int): SectorGroup = {
    // TODO include item/powerup rooms, but with nothing in them...
    ???
  }

  def getStart(ringIndex: Int, angleType: Int): SectorGroup = {
    ringIndex match {
      case InnerRing => throw new RuntimeException("can't have player start on inner ring - player ends up in an overlapping sector")
      case OuterRing => palette.playerStart(OuterRing, angleType)
    }
  }

  def getEnemy(ringIndex: Int, angleType: Int, threat: Int): SectorGroup = {
    val enemies = Seq(palette.droneDoors, palette.tripBombs)
    val section = random.randomElement(enemies.filter(_.hasRingIndex(ringIndex)))
    section(ringIndex, angleType)
  }

  def getDecor(ringIndex: Int, angleType: Int): SectorGroup = {
    // "decor" ->  "big mirror" => actual sector group
    ringIndex match {
      case InnerRing => palette.getBlank(ringIndex, angleType)
      case OuterRing => random.randomElement(Seq(1, 2, 3)) match {
        case 1 => palette.getBlank(ringIndex, angleType)
        case 2 => palette.screensOuterDecor(ringIndex, angleType)
        case 3 => palette.bigWindowsOuter(ringIndex, angleType)
        // case 3 => palette.outerConferenceRoom // TODO - but its only straight
      }
    }
  }

  def getBlank(ringIndex: Int, angleType: Int): SectorGroup = {
    ringIndex match {
      case InnerRing => palette.getBlank(ringIndex, angleType)
      case OuterRing => random.randomElement(Seq(1, 2)) match {
        case 1 => palette.getBlank(ringIndex, angleType)
        case 2 => palette.screensOuterDecor(ringIndex, angleType)
      }
    }
  }

  def getEnd(ringIndex: Int, angleType: Int, key2pal: Int): SectorGroup = {
    ringIndex match {
      case InnerRing => palette.getEnd(angleType, key2pal)
      case OuterRing => ???
    }
  }

}
