package trn.prefab.experiments.hyperloop

import trn.duke.{PaletteList, TextureList}
import trn.math.SnapAngle
import trn.prefab.experiments.ExpUtil
import trn.prefab.experiments.hyperloop.EdgeIds._
import trn.prefab.experiments.hyperloop.HyperLoop0.{calcRingAnchors, RingPrinter}
import trn.{PointXY, RandomX, HardcodedConfig, ScalaMapLoader, Sprite, AngleUtil}
import trn.prefab.{MapWriter, DukeConfig, GameConfig, SectorGroup, PrefabPalette, PastedSectorGroup, Marker}
import trn.prefab.experiments.hyperloop.HyperLoopParser._
import trn.prefab.experiments.hyperloop.RingLayout.{AXIS, DIAG}

import scala.collection.mutable






/**
  *
  * @param layout
  * @param count how many spaces around the ring (e.g. 8 for 1 loop, 16 for two loops, etc)
  *              only really used to link first and last
  */
class RingPrinter1(writer: MapWriter, layout: RingLayout, count: Int) {
  require(count % layout.anglesPer360 == 0)

  val angleIndexCount = count
  val ringCount = count / layout.anglesPer360

  val innerRing = mutable.Map[Int, PastedSectorGroup]()
  val middleRing = mutable.Map[Int, PastedSectorGroup]()
  val outerRing = mutable.Map[Int, PastedSectorGroup]()

  def pasteMiddle(index: Int, midSg: SectorGroup): PastedSectorGroup = {
    require(index < count)
    if(middleRing.contains(index)){
      throw new RuntimeException(s"there is already a middle ring at index ${index}")
    }

    val heading = RingLayout.indexToHeading(index)
    val sg = RingLayout.rotationToHeading(heading).rotate(midSg)
    val loc = layout.midRingAnchors(heading).withZ(0)
    val psg = writer.pasteSectorGroupAt(sg, loc, true)
    middleRing.put(index, psg)
    psg
  }

  private def pasteAndLinkToMid(index: Int, midEdgeId: Int, newSg: SectorGroup, newSgEdgeId: Int): PastedSectorGroup = {
    val midPsg = middleRing.get(index).getOrElse(throw new Exception(s"cannot paste innerSg at index ${index}, there is no middle sg to latch onto"))
    writer.pasteAndLink(midPsg.getRedwallConnector(midEdgeId), newSg, newSg.getRedwallConnector(newSgEdgeId), Seq.empty)
  }

  def pasteInner(index: Int, innerSg: SectorGroup): PastedSectorGroup = {
    require(index < count)
    if(innerRing.contains(index)){
      throw new RuntimeException(s"there is already an inner ring at index ${index}")
    }
    val heading = RingLayout.indexToHeading(index)
    val sg = RingLayout.rotationToHeading(heading).rotate(innerSg)
    val psg = pasteAndLinkToMid(index, InnerEdgeConn, sg, OuterEdgeConn)
    innerRing.put(index, psg)
    psg
  }

  def pasteOuter(index: Int, outerSg: SectorGroup): PastedSectorGroup = {
    require(index < count)
    if(outerRing.contains(index)){
      throw new RuntimeException(s"there is already an outer ring at index ${index}")
    }
    // val midPsg = middleRing.get(index).getOrElse(throw new Exception(s"cannot paste innerSg at index ${index}, there is no middle sg to latch onto"))
    val heading = RingLayout.indexToHeading(index)
    val sg = RingLayout.rotationToHeading(heading).rotate(outerSg)

    val psg = pasteAndLinkToMid(index, OuterEdgeConn, sg, InnerEdgeConn)
    outerRing.put(index, psg)
    psg
  }

  def tryPasteOuter(index: Int, outerSg: SectorGroup): Option[PastedSectorGroup] = {
    require(index < count)
    if(!outerRing.contains(index)){
      Some(pasteOuter(index, outerSg))
    }else{
      None
    }
  }

  def tryPasteMid(index: Int, midSg: SectorGroup): Option[PastedSectorGroup] = {
    require(index < count)
    if(!middleRing.contains(index)){
      Some(pasteMiddle(index, midSg))
    }else{
      None
    }
  }

  def tryPasteInner(index: Int, innerSg: SectorGroup): Option[PastedSectorGroup] = if(!innerRing.contains(index)) {
    Some(pasteInner(index, innerSg))
  }else{
    None
  }

  def pasteSection(index: Int, ringSection: RingSection): Unit = {
    require(index < count)
    val heading = RingLayout.indexToHeading(index)
    if(ringSection.angleType != RingLayout.angleType(heading)){
      throw new RuntimeException(s"wrong index for section type")
    }

    // mid must be first
    ringSection.mid.foreach(sg => pasteMiddle(index, sg))
    ringSection.inner.foreach(sg => pasteInner(index, sg))
    ringSection.outer.foreach(sg => pasteOuter(index, sg))
  }

  /**
    * Fill in empty spaces with default sector groups
    */
  def fillInner(axisSg: SectorGroup, diagonalSg: SectorGroup): Unit = {
    (0 until count).foreach { index =>
      val heading = RingLayout.indexToHeading(index)
      if(middleRing.contains(index) && !innerRing.contains(index)){
        val sg = if(RingLayout.axisAligned(heading)){ axisSg } else { diagonalSg }
        pasteInner(index, sg)
      }
    }
  }

  /**
    * Fill in empty spaces with default sector groups
    */
  def fillOuter(axisSg: SectorGroup, diagonalSg: SectorGroup): Seq[(Int, PastedSectorGroup)] = {
    (0 until count).flatMap { index =>
      val heading = RingLayout.indexToHeading(index)
      if (middleRing.contains(index) && !outerRing.contains(index)) {
        val sg = if (RingLayout.axisAligned(heading)) { axisSg } else { diagonalSg }
        val psg = pasteOuter(index, sg)
        Some(index, psg)
      }else{
        None
      }

    }
  }

  def autoLink(): Unit = {
    autoLinkRing(innerRing)
    autoLinkRing(middleRing)
    autoLinkRing(outerRing)
  }

  /**
    * Automatically link PSGs together, taking care to only link ones that are next to each other (important because
    * many different sectors will be sitting on top of each other)
    */
  private def autoLinkRing(ring: mutable.Map[Int, PastedSectorGroup]): Unit = {
    val pairs: Seq[Seq[Option[PastedSectorGroup]]] = (0 until count).map(ring.get).sliding(2).toSeq
    val pairs2 = pairs ++ Seq(Seq(ring.get(count - 1), ring.get(0)))
    pairs2.foreach { pair =>
      require(pair.size == 2)
      val a = pair(0)
      val b = pair(1)
      if(a.isDefined && b.isDefined){
        writer.autoLink(a.get, b.get)
      }
    }

  }

  /**
    * Adds a touchplate to a middle ring sector group
    * @param psg must be a mid group
    * @param lotag the lotag of the touchplate
    */
  def addTouchplate(psg: PastedSectorGroup, lotag: Int): Unit = {
    val loc = psg.boundingBox.center
    val sectorId = psg.allSectorIds.head // TODO making an assumption here
    val touch = SpriteFactory.touchplate(loc.withZ(0), sectorId, lotag)
    psg.map.addSprite(touch)
  }

}


case class RingSection(angleType: Int, inner: Option[SectorGroup], mid: Option[SectorGroup], outer: Option[SectorGroup])

object HyperLoop1 {
  val Filename = "loop1.map"
  val SpaceFilename = "SPACE.MAP" // tried to make an input of map of "standard" space things

  // TODO - do a cool effect with the cycler
  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    run2(gameCfg)
  }

  def allSwitchRequests(psg: PastedSectorGroup): Seq[Int] = {
    psg.allSpritesInPsg.filter(s => Marker.isMarker(s, Marker.Lotags.SWITCH_REQUESTED)).map(_.getHiTag)
  }

  def run(gameCfg: GameConfig): Unit = {

    // TODO ideas
    // X- add a room for the start position
    // X- cool effect with the cycler (tex 276)
    // X- hallway blocked by force field
    // X- mirror!
    // space bathroom (make it work from either axis or diag)
    // -- auto fill in wall panels using that stuff i tried to invent for moonbase2/3
    // -- room full of rats (use marker to autofill)
    // -- subway going around the outer level!
    // -- switch between two different styles of outer groups (e.g. one uses a multisector connector)
    // -- central traversals
    //     - vents
    //     - shrink-ray-only passages
    //     - normal doors/hallways
    //     - blow way through via cracks
    // multi layered rotating shields protecting a reactor

    // -- something to make it easier to see that its more than one loop (half circle decorations?)

    val palette = ScalaMapLoader.loadPalette(HardcodedConfig.EDUKE32PATH + Filename, Some(gameCfg))
    val spacePal = ScalaMapLoader.loadPalette(HardcodedConfig.EDUKE32PATH + SpaceFilename, Some(gameCfg))
    val writer = MapWriter(gameCfg)
    val loop1Palette = new Loop1(gameCfg, new RandomX(), palette, spacePal)
    val ringLayout = RingLayout.fromSectorGroups(loop1Palette.core, loop1Palette.innerE, loop1Palette.midE)
    val ringPrinter = new RingPrinter1(writer, ringLayout, 24)

    // FORCE FIELD
    ringPrinter.pasteSection(1, loop1Palette.forceFieldDiag)
    val forceChannel = allSwitchRequests(ringPrinter.middleRing(1)).head
    ringPrinter.outerRing(1).allSpritesInPsg.filter(s => s.getTex == TextureList.Switches.ACCESS_SWITCH_2).foreach { switch =>
      switch.setLotag(forceChannel)
    }

    ringPrinter.pasteMiddle(8, loop1Palette.midBigDoor)
    ringPrinter.pasteInner(8, loop1Palette.innerBlocked)
    ringPrinter.pasteOuter(8, loop1Palette.outerBlocked)

    // fill in middle ring
    for (index <- 0 until ringPrinter.angleIndexCount) {
      if(!ringPrinter.middleRing.contains(index)){
        val angleType = RingLayout.indexToAngleType(index)
        ringPrinter.pasteMiddle(index, loop1Palette.defaultMid(angleType))
      }
    }
    // ringPrinter.pasteOuter(0, loop1Palette.outerMirror) // TODO
    // ringPrinter.pasteOuter(3, loop1Palette.outerDiagStraightExtender) // TODO
    ringPrinter.pasteOuter(4, loop1Palette.outerStart)
    ringPrinter.pasteOuter(6, loop1Palette.outerRoomWithItem(TextureList.Items.KEY))
    // ringPrinter.pasteInner(7, loop1Palette.innerEndDiag) // TODO
    ringPrinter.pasteSection(10, loop1Palette.sectionWithKey)


    val pastedGuns = ringPrinter.pasteInner(0, loop1Palette.innerSurpriseGuns)
    val switchLotag = allSwitchRequests(pastedGuns).head //val switchLotag = pastedGuns.allSpritesInPsg.find(s => Marker.isMarker(s, Marker.Lotags.SWITCH_REQUESTED)).get.getHiTag
    ringPrinter.addTouchplate(ringPrinter.middleRing(0), switchLotag) // TODO should this be a method on the printer?

    ringPrinter.fillInner(loop1Palette.innerE, loop1Palette.innerSE)
    // ringPrinter.fillOuter(outerE, outerSE)


    //
    // CYCLERS
    //
    // val pastedLights = ringPrinter.fillOuter(loop1Palette.outerLightPulse, loop1Palette.outerLightsDiag)
    (0 until 8).foreach { index =>
      ringPrinter.tryPasteOuter(index, loop1Palette.defaultOuter(RingLayout.indexToAngleType(index)))
    }
    (8 until 16).foreach { index =>
      ringPrinter.tryPasteOuter(index, loop1Palette.outerLights(RingLayout.indexToAngleType(index)))
    }
    (16 until 24).foreach { index =>
      ringPrinter.tryPasteOuter(index, loop1Palette.outerLights(RingLayout.indexToAngleType(index), true))
    }
    // val pastedLights = ringPrinter.fillOuter(loop1Palette.outerLights(RingLayout.AXIS), loop1Palette.outerLights(RingLayout.DIAG))


    // pastedLights.foreach { case (index, psg) =>
    //   setupCyclers(psg, RingLayout.indexToHeading(index), 128)
    // }
    setupAllCyclers(ringPrinter)

    ringPrinter.autoLink()
    ExpUtil.finishAndWrite(writer)
  }

  def run2(gameCfg: GameConfig): Unit = {
    val random = new RandomX()
    val palette = ScalaMapLoader.loadPalette(HardcodedConfig.EDUKE32PATH + Filename, Some(gameCfg))
    val spacePal = ScalaMapLoader.loadPalette(HardcodedConfig.EDUKE32PATH + SpaceFilename, Some(gameCfg))
    val writer = MapWriter(gameCfg)
    val loop1Palette = new Loop1(gameCfg, random, palette, spacePal)
    val ringLayout = RingLayout.fromSectorGroups(loop1Palette.core, loop1Palette.innerE, loop1Palette.midE)
    val ringPrinter = new RingPrinter1(writer, ringLayout, 24)


    // blue gate
    ringPrinter.pasteSection(15, loop1Palette.forceFieldDiag2())

    // red gate
    ringPrinter.pasteSection(9, loop1Palette.forceFieldDiag2(true))

    // door between red and clear sections
    ringPrinter.pasteSection(0, loop1Palette.sectionBlockedByDoor(RingLayout.AXIS))


    // fill in middle ring
    for (index <- 0 until ringPrinter.angleIndexCount) {
      if (!ringPrinter.middleRing.contains(index)) {
        val angleType = RingLayout.indexToAngleType(index)
        ringPrinter.pasteMiddle(index, loop1Palette.defaultMid(angleType))
      }
    }

    def getTileset(index: Int): Int = if(16 <= index && index < 24){ L1Tileset.Clear } else { L1Tileset.Secure}

    // random
    Seq(18, 17, 10, 11).foreach { i =>
      val atype = RingLayout.indexToAngleType(i)
      val tileset = getTileset(i)
      ringPrinter.pasteOuter(i, loop1Palette.outerItemArea(atype, tileset))
    }

    // blue key
    ringPrinter.pasteOuter(22, loop1Palette.outerKeyArea(AXIS, PaletteList.KEYCARD_BLUE)) // TODO random

    // start // TODO must be far away, to prevent starting in the wrong sector
    ringPrinter.pasteOuter(20, loop1Palette.playerStartOuter(RingLayout.AXIS))

    // red key
    // TODO get a DIAG outer key area, so I can use random!
    ringPrinter.pasteOuter(12, loop1Palette.outerKeyArea(AXIS, PaletteList.KEYCARD_RED))  // TODO random
    // TODO many doors, so the player doesnt know where the key is!


    // yellow key (1 to 8)
    ringPrinter.pasteOuter(2, loop1Palette.outerKeyArea(AXIS, PaletteList.KEYCARD_YELLOW))  // TODO random


    // end
    ringPrinter.pasteInner(23, loop1Palette.innerEndDiag.withKeyLockColor(gameCfg, PaletteList.KEYCARD_YELLOW))


    //
    // Fill in Outer
    //
    // zero section:
    (16 until 24).foreach { index =>
      ringPrinter.tryPasteInner(index, loop1Palette.innerDefaultTileset(RingLayout.indexToAngleType(index)))
      ringPrinter.tryPasteOuter(index, loop1Palette.defaultOuter(RingLayout.indexToAngleType(index)))
    }
    // blue section (normal size would be 8 to 16
    (10 until 16).foreach { index =>
      ringPrinter.tryPasteInner(index, loop1Palette.innerLightsTileset(RingLayout.indexToAngleType(index)))
      ringPrinter.tryPasteOuter(index, loop1Palette.outerLights(RingLayout.indexToAngleType(index)))
    }
    // red section
    (0 until 10).foreach { index =>
      ringPrinter.tryPasteInner(index, loop1Palette.innerLightsTileset(RingLayout.indexToAngleType(index)))
      ringPrinter.tryPasteOuter(index, loop1Palette.outerLights(RingLayout.indexToAngleType(index), true))
    }
    setupAllCyclers(ringPrinter)

    ringPrinter.autoLink()
    ExpUtil.finishAndWrite(writer)
  }


  val CyclerOffsets = Map(
    RingLayout.East -> 0,
    RingLayout.NorthEast -> 12, // because the east one had 12...
    RingLayout.North-> 22, // because the northeast one added 10
    RingLayout.NorthWest -> 34,
    RingLayout.West -> 44,
    RingLayout.SouthWest -> 56,
    RingLayout.South -> 66,
    RingLayout.SouthEast -> 78,
  )

  def setupAllCyclers(ringPrinter: RingPrinter1): Unit = {
    ringPrinter.outerRing.foreach { case (index, psg) =>
      if (psg.allSpritesInPsg.find(_.getTex == TextureList.CYCLER).isDefined) {
        setupCyclers(psg, RingLayout.indexToHeading(index), 128)
      }
    }

  }

  /**
    * this is for two specific sector groups, to make it look like there is a unified light pulse all
    * the way around the ring
    *
    * the cyclers in these groups are actually arranged anticlockwise.  The axis-aligned groups have 12,
    * and the diagonal ones have 10.
    */
  def setupCyclers(cyclerPsg: PastedSectorGroup, heading: Int, pulseSpeed: Int = 68): Unit = {
    val CyclerCount360 = 88 // there are 88 cyclers in a single circle (there are more than 88 on the map,
    // because the ring wraps around multiple times)
    val TotalOffset = 8192 // calc for one beam, b/c I'm using math that pretends its a single, normal loop
    val OffsetDelta = TotalOffset / CyclerCount360

    // this was the bug -- returns all sprites in the map!
    // TODO should probably switch to setting these in the SectorGroup before pasting!
    val cyclersInPsg = cyclerPsg.allSprites.filter { s =>
      s.getTex == TextureList.CYCLER && cyclerPsg.allSectorIds.contains(s.getSectorId)
    }
    val cyclers = cyclersInPsg.sortBy(s => s.getLotag) // don't reverse it here
    // require(cyclers.size == 10 || cyclers.size == 12)

    for (i <- 0 until cyclers.size){
      val cycler = cyclers(i)

      // val globalIndex = i + CyclerOffsets(heading)
      val globalIndex = CyclerOffsets(heading) + cycler.getLotag

      val globalIndex2 = CyclerCount360 - globalIndex // reverse it
      require(globalIndex2 >= 0)

      val pulseOffset = globalIndex2 * OffsetDelta
      // val pulseOffset = i * (2048 / 10)
      cycler.setLotag(pulseOffset)
      cycler.setAng(AngleUtil.ANGLE_UP)

      val gpspeed = cyclerPsg.allSprites.find(s => s.getTex == TextureList.GPSSPEED && s.getSectorId == cycler.getSectorId).get
      gpspeed.setLotag(pulseSpeed)
    }
  }
}
