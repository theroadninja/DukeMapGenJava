package trn.prefab.experiments

import trn.duke.{PaletteList, TextureList}
import trn.math.SnapAngle
import trn.{PointXYZ, PointXY, prefab, ScalaMapLoader, Main, HardcodedConfig, LineSegmentXY, Sprite, AngleUtil}
import trn.prefab.{experiments, CompassWriter, MapWriter, DukeConfig, GameConfig, SectorGroup, RedwallConnector, PastedSectorGroup}

import scala.collection.mutable

object RingHeadings {
  // 0 == no id
  val East = 1
  val SouthEast = 2
  val South = 3
  val SouthWest = 4
  val West = 5
  val NorthWest = 6
  val North = 7
  val NorthEast = 8

  val AxisAligned = Set(East, South, West, North)

  def clockwise(heading: Int): Int = {
    val h2 = heading + 1
    if(h2 > 8){ 1 } else { h2 }
  }

  def anticlockwise(heading: Int): Int = {
    val h2 = heading - 1
    if(h2 < 1){ 8 } else { h2 }
  }

  def axisAligned(heading: Int): Boolean = AxisAligned.contains(heading)
}


object HyperLoop0 {
  val Filename = "loop0.map"


  /**
    * Connector that faces the center of the circle.
    */
  val InnerEdgeConn = 1

  /**
   * Connector id of the anticlockwise edge.
   * If you are inside the group facing anitclockwise, you are looking at this edge
   */
  val AntiClockwiseEdge = 2

  /**
    * Connector id of the clockwise edge.
    * If you are inside the group facing anitclockwise, you are looking at this edge
    */
  val ClockwiseEdge = 3

  val OuterEdgeConn = 4

  /**
    * Calculates the anchors used to place the ring groups.   The diagonal SG anchors are lined up
    * with the straight SGs, so it ends up just being a box
    *
    * @param innerRadius determined by size of inner core
    * @param innerWallToAnchor distance from inner edge to anchor sprite
    * @return
    */
  def calcRingAnchors(innerRadius: Int, innerWallToAnchor: Int): Map[Int, PointXY] = {
    val radius = innerRadius + innerWallToAnchor
    Map(
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

  object RingPrinter {

    def rotateEastTo(sg: SectorGroup, heading: Int) = heading match {
      // this assumes the sectorGroup is an "east" group
      case RingHeadings.East => sg
      case RingHeadings.South => sg.rotateCW
      case RingHeadings.West => sg.rotateCW.rotateCW
      case RingHeadings.North => sg.rotateCW.rotateCW.rotateCW
      // this assumes the SectorGroup is a "southeast" group
      case RingHeadings.SouthEast => sg
      case RingHeadings.SouthWest => sg.rotateCW
      case RingHeadings.NorthWest => sg.rotateCW.rotateCW
      case RingHeadings.NorthEast => sg.rotateCW.rotateCW.rotateCW
    }
  }

  class RingPrinter(writer: MapWriter, ringAnchorPoints: Map[Int, PointXY], startHeading: Int) {

    // in clockise order
    val pastedGroups = mutable.ListBuffer[(PastedSectorGroup, Int)]()

    def lastClockwise: Option[(PastedSectorGroup, Int)] = pastedGroups.lastOption
    def lastAntiClockwise: Option[(PastedSectorGroup, Int)] = pastedGroups.headOption

    private def printSg(
      sg: SectorGroup,
      clockwise: Boolean = true,
    ): PastedSectorGroup = {
      val (otherPsg, heading) = if(pastedGroups.isEmpty){
        (None, startHeading)
      }else if(clockwise){
        val (prevPsg, prevHeading) = lastClockwise.get
        (Some(prevPsg), RingHeadings.clockwise(prevHeading))
      }else{
        val (prevPsg, prevHeading) = lastAntiClockwise.get
        (Some(prevPsg), RingHeadings.anticlockwise(prevHeading))
      }

      val loc: PointXYZ = ringAnchorPoints(heading).withZ(0)
      val psg = writer.pasteSectorGroupAt(sg, loc, true)
      otherPsg.foreach(psg2 => writer.autoLink(psg, psg2))
      if(clockwise){
        pastedGroups.append((psg, heading))
      }else{
        pastedGroups.prepend((psg, heading))
      }
      psg
    }

    def autolinkEnds(): Unit = {

      writer.autoLink(lastClockwise.get._1, lastAntiClockwise.get._1)
    }

    def nextClockwiseHeading: Option[Int] = lastClockwise.map(_._2).map(RingHeadings.clockwise)

    def printClockwise(sg: SectorGroup): PastedSectorGroup = printSg(sg)


    def rotateAndPrintClockwise(sg: SectorGroup): PastedSectorGroup = {
      val sg2 = RingPrinter.rotateEastTo(sg, nextClockwiseHeading.getOrElse(startHeading))
      printSg(sg2)
    }

    def printAntiClockwise(sg: SectorGroup): PastedSectorGroup = printSg(sg, false)

  }

  // TODO - do a cool effect with the cycler
  def main(args: Array[String]): Unit = {

    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    threeLayerRing(gameCfg)


  }

  /**
    * Treat the sequence of PastedSectorGroups as if it represents a ring of groups that should all be connected,
    * including the first to the last.
    * @param writer
    * @param ring
    */
  def autoLinkRing(writer: MapWriter, ring: Seq[PastedSectorGroup]): Unit = {
    ring.sliding(2).foreach { win =>
      require(win.size == 2)
      writer.autoLink(win.head, win.last)
    }
    writer.autoLink(ring.head, ring.last)
  }

  /**
    * measure the distance from the "inner" connector to the anchor.  Only works with axis-aligned ring groups
    *
    * With this algorithm, only the "middle" ring sector groups can have anchors.
    *
    * @param sg - should be an "east" axis-aligned ring group
    * @return
    */
  def measureDistToAnchor(sg: SectorGroup): Int = {
    val conn = sg.getRedwallConnector(InnerEdgeConn)
    require(conn.isAxisAligned, "connector must be axis-aligned")
    sg.getRedwallConnector(InnerEdgeConn).getBoundingBox.center.manhattanDistanceTo(sg.getAnchor.asXY).toInt
  }

  def threeLayerRing(gameCfg: GameConfig): Unit = {
    val palette = ScalaMapLoader.loadPalette(HardcodedConfig.EDUKE32PATH + Filename, Some(gameCfg))
    val writer = MapWriter(gameCfg)

    val innerE = palette.getSG(11)
    val midE = palette.getSG(12)
    val outerE = palette.getSG(13)

    val innerSE = palette.getSG(14)
    val midSE = palette.getSG(15)
    val outerSE = palette.getSG(16)

    val centerToInnerEdgeOfMid = 4608

    // val distToAnchor = measureDistToAnchor(midE)
    // println(s"bb=${midE.getRedwallConnector(InnerEdgeConn).getBoundingBox}")
    // println(s"distToAnchor=${distToAnchor}")
    val midRingAnchors = calcRingAnchors(centerToInnerEdgeOfMid, measureDistToAnchor(midE))

    val ringPrinter = new RingPrinter(writer, midRingAnchors, RingHeadings.East)

    def selectRing(index: Int): SectorGroup = if(index% 2 == 0){ midE } else { midSE }

    for (index <- 0 until 16) {
      val psg = ringPrinter.rotateAndPrintClockwise(selectRing(index))
    }

    ringPrinter.autolinkEnds

    val innerPsgs = ringPrinter.pastedGroups.map { case (midPsg, heading) =>
      val innerSg = if(RingHeadings.axisAligned(heading)){
        innerE
      }else{
        innerSE
      }
      val innerSg2 = RingPrinter.rotateEastTo(innerSg, heading)
      writer.pasteAndLink(
        midPsg.getRedwallConnector(InnerEdgeConn),
        innerSg2,
        innerSg2.getRedwallConnector(OuterEdgeConn),
        Seq.empty,
      )
    }
    autoLinkRing(writer, innerPsgs)


    val outerPsgs = ringPrinter.pastedGroups.map { case (midPsg, heading) =>
      val outerSg = if (RingHeadings.axisAligned(heading)) {
        outerE
      } else {
        outerSE
      }
      val outerSg2 = RingPrinter.rotateEastTo(outerSg, heading)
      writer.pasteAndLink(
        midPsg.getRedwallConnector(OuterEdgeConn),
        outerSg2,
        outerSg2.getRedwallConnector(InnerEdgeConn),
        Seq.empty,
      )
    }
    autoLinkRing(writer, outerPsgs)


    ExpUtil.finishAndWrite(writer)

  }

  def simpleRing(gameCfg: GameConfig): Unit = {
    val palette = ScalaMapLoader.loadPalette(HardcodedConfig.EDUKE32PATH + Filename, Some(gameCfg))
    val writer = MapWriter(gameCfg)

    // This worked, but can't use a solid core (run out of spots to connect after 1 loop!
    // val sgCenter = palette.getSG(1)
    // val sgCenterAnchor = sgCenter.boundingBox.center
    // writer.pasteSectorGroupAtCustomAnchor(sgCenter, PointXYZ.ZERO, sgCenterAnchor.withZ(0))

    val eastSg = palette.getSG(2)
    val southEastSg = palette.getSG(3)

    val eastSgDivider = palette.getSG(4)

    // val center = PointXY.ZERO
    val InnerRadius = 1536 // determined by size of inner core
    val edgeToAnchor = 2048 // distance from inner edge to anchor sprite
    // val eastLocation = new PointXY(InnerRadius + edgeToAnchor, 0).withZ(0)

    val ringAnchors = calcRingAnchors(InnerRadius, edgeToAnchor)
    val ringPrinter = new RingPrinter(writer, ringAnchors, RingHeadings.East)

    // val font = TextureList.getFont(2966)
    val font = TextureList.FONT_BIGGRAY
    val s = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"


    def selectRingSg(index: Int): SectorGroup = {
      if (index == 0 || index == 8) {
        eastSgDivider
      } else {
        val sg = if (index % 2 == 0) {
          eastSg
        } else {
          southEastSg
        }

        if (index > 8) {
          sg.withTexturesReplaced(Map(258 -> 252))
          // this doesnt affect tex 258 the way I thought it would:
          // MapWriter.painted2(sg, PaletteList.BLUE_TO_RED)
        } else {
          sg
        }

      }
    }

    for (index <- 0 until 16) {
      val psg = ringPrinter.rotateAndPrintClockwise(selectRingSg(index))
      //psg.boundingBox.center
      // val tex = font.textureFor(s(index).toString)
      // val sectorId: Int = psg.sectorIds.head
      // val numberSprite = new Sprite(psg.boundingBox.center.withZ(0), sectorId, tex, 0, 0)
      // writer.getMap.addSprite(numberSprite)
    }


    ringPrinter.autolinkEnds

    // DOESNT WORK, especially for SW
    // SnapAngle.rotateUntil(eastSg){  sg =>
    //   sg.getRedwallConnector(InnerEdgeConn).getHeading
    // }

    // val psgEast = writer.pasteSectorGroupAt(eastSg, ringAnchors(RingHeadings.East).withZ(0), true)
    // val psgSE = writer.pasteSectorGroupAt(southEastSg, ringAnchors(RingHeadings.SouthEast).withZ(0), true)
    // writer.autoLink(psgEast, psgSE)

    ExpUtil.finishAndWrite(writer)

  }

}
