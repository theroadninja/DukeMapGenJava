package trn.prefab.experiments.hyperloop

import trn.duke.TextureList
import trn.math.SnapAngle
import trn.prefab.experiments.ExpUtil
import trn.prefab.experiments.hyperloop.EdgeIds._
import trn.prefab.experiments.hyperloop.HyperLoop0.{calcRingAnchors, RingPrinter}
import trn.{PointXY, HardcodedConfig, ScalaMapLoader, Sprite, AngleUtil}
import trn.prefab.{MapWriter, DukeConfig, GameConfig, SectorGroup, PastedSectorGroup, Marker}
import trn.prefab.experiments.hyperloop.HyperLoopParser._

import scala.collection.mutable


/**
  * A ring-based layout that uses 8 fixed angles matching standard compass headings.
  *
  * @param innerRadius radius from the center to the inner wall of the middle ring
  * @param midWallToAnchor distance from the inner wall of the middle ring to the anchor
  *                        (of an axis-aligned mid ring SG)
  */
class RingLayout(
  innerRadius: Int,
  midWallToAnchor: Int,
) {
  // this layout has exactly 8 discrete angles that add up to 360 degrees
  val anglesPer360: Int = 8

  val radius = innerRadius + midWallToAnchor
  val midRingAnchors = Map(
    RingHeadings.East -> new PointXY(radius, 0),
    RingHeadings.SouthEast -> new PointXY(radius, radius),
    RingHeadings.South -> new PointXY(0, radius),
    RingHeadings.SouthWest -> new PointXY(-radius, radius),
    RingHeadings.West -> new PointXY(-radius, 0),
    RingHeadings.NorthWest -> new PointXY(-radius, -radius),
    RingHeadings.North -> new PointXY(0, -radius),
    RingHeadings.NorthEast -> new PointXY(radius, -radius),
  )

}

object RingLayout {

  //
  // Types of Angles
  // (that can be transformed between using 90 degree rotations)
  //
  /** East, South, West, North */
  val AXIS = 0

  /** Southeast, Southwest, Northwest, Northeast */
  val DIAG = 1

  //
  // Headings (there can be infinite angles going up to infinity, but only 8 headings)
  // 1-based, in case I want to put them in hitags/lotags
  //
  val East = 1
  val SouthEast = 2
  val South = 3
  val SouthWest = 4
  val West = 5
  val NorthWest = 6
  val North = 7
  val NorthEast = 8

  val Headings = Seq(East, SouthEast, South, SouthWest, West, NorthWest, North, NorthEast)

  /**
    * @param angleIndex an index of polar angles, starting at 0 for east and going clockwise
    * @return the enum value for the compass angle, e.g. South, Northwest, etc
    */
  def indexToHeading(angleIndex: Int): Int = {
    require(angleIndex >= 0)
    Headings(angleIndex % 8)
  }

  val AxisAligned = Set(East, South, West, North)

  /**
    * Create a ring layout using paramaters measured from the given sector groups
    * @param coreSg a single-sector group with the shape that matches the area inside the inner ring
    * @param innerSg an east-facing "inner ring" sector group, with two Y-axis-aligned walls that can be used to measure
    *                the width of the inner ring (inner rings are allowed to have things that stick into the middle, so
    *                need to pass one that doesnt do that.
    * @param midSg an east-facing "middle ring" sector group
    * @return
    */
  def fromSectorGroups(coreSg: SectorGroup, innerSg: SectorGroup, midSg: SectorGroup): RingLayout = {
    val centerToInnerEdgeOfMid = measureWidth(coreSg) / 2 + measureWidth(innerSg)
    new RingLayout(centerToInnerEdgeOfMid, measureDistToAnchor(midSg))
  }

  def clockwise(heading: Int): Int = {
    val h2 = heading + 1
    if (h2 > 8) {
      1
    } else {
      h2
    }
  }

  def anticlockwise(heading: Int): Int = {
    val h2 = heading - 1
    if (h2 < 1) {
      8
    } else {
      h2
    }
  }

  def axisAligned(heading: Int): Boolean = AxisAligned.contains(heading)

  /**
    * Assuming you are starting with something facing East OR Southeat, return the number of 90 degree
    * rotations  to read that heading.
    *
    * @param destAngle destination angle you want to rotate to
    * @return a SnapAngle, the number of 90 degree rotations you need to make
    */
  def rotationToHeading(destHeading: Int): SnapAngle = destHeading match {
    // assumes you are starting from "East"
    case RingHeadings.East => SnapAngle(0)
    case RingHeadings.South => SnapAngle(1)
    case RingHeadings.West => SnapAngle(2)
    case RingHeadings.North => SnapAngle(3)
    // assumes you are starting from "Southeast"
    case RingHeadings.SouthEast => SnapAngle(0)
    case RingHeadings.SouthWest => SnapAngle(1)
    case RingHeadings.NorthWest => SnapAngle(2)
    case RingHeadings.NorthEast => SnapAngle(3)
  }

}

/**
  *
  * @param layout
  * @param count how many spaces around the ring (e.g. 8 for 1 loop, 16 for two loops, etc)
  *              only really used to link first and last
  */
class RingPrinter1(writer: MapWriter, layout: RingLayout, count: Int) {

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

object HyperLoop1 {
  val Filename = "loop1.map"

  // TODO - do a cool effect with the cycler
  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    run(gameCfg)
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
    // -- vent on inner wall connects to annother vent on inner wall  (alternative:  crack, door)
    // multi layered rotating shields protecting a reactor

    // -- something to make it easier to see that its more than one loop (half circle decorations?)

    val palette = ScalaMapLoader.loadPalette(HardcodedConfig.EDUKE32PATH + Filename, Some(gameCfg))
    val writer = MapWriter(gameCfg)

    val core = palette.getSG(1)
    val innerSurpriseGuns = palette.getSG(2)
    require(innerSurpriseGuns.props.switchesRequested.size == 1)

    val outerStart = palette.getSG(4)
    val outerLightPulse = palette.getSG(5) // first attempt id=3
    val outerLightsDiag = palette.getSG(6)
    val outerMirror = palette.getSG(7)

    val midForceFieldDiag = palette.getSG(8)
    val outerForceFieldDiag = palette.getSG(9)
    val innerForceFieldDiag = palette.getSG(10)

    //
    // standard groups
    //
    val innerE = palette.getSG(11)
    val midE = palette.getSG(12)
    val outerE = palette.getSG(13)

    val innerSE = palette.getSG(14)
    val midSE = palette.getSG(15)
    val outerSE = palette.getSG(16)

    // just an outer diag that makes it look like the adjacent (on the anticlockwise side) straigt wall
    // is extended farther
    val outerDiagStraightExtender = palette.getSG(17)


    val ringLayout = RingLayout.fromSectorGroups(core, innerE, midE)
    val ringPrinter = new RingPrinter1(writer, ringLayout, 16)

    def selectRing(index: Int): SectorGroup = if (index % 2 == 0) {
      midE
    } else {
      midSE
    }

    def selectOuterRing(index: Int): SectorGroup = if(index % 2 == 0) {
      outerE
    } else {
      outerSE
    }

    val forceMid = ringPrinter.pasteMiddle(1, midForceFieldDiag)
    val forceChannel = allSwitchRequests(forceMid).head
    val pastedForceOuter = ringPrinter.pasteOuter(1, outerForceFieldDiag)
    pastedForceOuter.allSpritesInPsg.filter(s => s.getTex == TextureList.Switches.ACCESS_SWITCH_2).foreach { switch =>
      switch.setLotag(forceChannel)
    }
    ringPrinter.pasteInner(1, innerForceFieldDiag)

    for (index <- 0 until 16) {
      if(!ringPrinter.middleRing.contains(index)){
        ringPrinter.pasteMiddle(index, selectRing(index))
      }
    }
    ringPrinter.pasteOuter(4, outerStart)
    ringPrinter.pasteOuter(0, outerMirror)
    ringPrinter.pasteOuter(3, outerDiagStraightExtender)

    // (0 until 16).foreach { index => ringPrinter.pasteOuter(index, selectOuterRing(index))}

    val pastedGuns = ringPrinter.pasteInner(0, innerSurpriseGuns)
    //val pastedGuns = ringPrinter.pasteInside(0, innerSurpriseGuns)
    val switchLotag = allSwitchRequests(pastedGuns).head //val switchLotag = pastedGuns.allSpritesInPsg.find(s => Marker.isMarker(s, Marker.Lotags.SWITCH_REQUESTED)).get.getHiTag

    ringPrinter.addTouchplate(ringPrinter.middleRing(0), switchLotag) // TODO should this be a method on the printer?

    ringPrinter.fillInner(innerE, innerSE)
    // ringPrinter.fillOuter(outerE, outerSE)


    //
    // CYCLERS
    //
    val pastedLights = ringPrinter.fillOuter(outerLightPulse, outerLightsDiag)
    pastedLights.foreach { case (index, psg) =>
      setupCyclers(psg, RingLayout.indexToHeading(index), 128)
    }

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
    // val PulseSpeed = 68 // should be about one every 2 seconds

    // this was the bug -- returns all sprites in the map!
    // TODO should probably switch to setting these in the SectorGroup before pasting!
    val cyclersInPsg = cyclerPsg.allSprites.filter { s =>
      s.getTex == TextureList.CYCLER && cyclerPsg.allSectorIds.contains(s.getSectorId)
    }
    val cyclers = cyclersInPsg.sortBy(s => s.getLotag) // don't reverse it here
    require(cyclers.size == 10 || cyclers.size == 12)

    for (i <- 0 until cyclers.size){
      val cycler = cyclers(i)
      val globalIndex = i + CyclerOffsets(heading)

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
