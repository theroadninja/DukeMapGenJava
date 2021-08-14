package trn.bespoke.moonbase2

import trn.{BuildConstants, FVectorXY, HardcodedConfig, LineSegmentXY, LineXY, Main, MapLoader, MapUtil, PlayerStart, PointXY, PointXYZ, Sector, Wall, WallView, Map => DMap}
import trn.prefab.{BoundingBox, DukeConfig, GameConfig, Heading, MapWriter, RandomX}
import trn.render.{HorizontalBrush, MiscPrinter, Texture, TextureUtil, WallAnchor, WallPrefab}
import trn.render.MiscPrinter.wall

import scala.collection.JavaConverters._
import scala.collection.mutable
import trn.PointImplicits._



/** TODO replaced by HorizontalBrush? */
object SectorPrefab {
  val DefaultFloorZ = Sector.DEFAULT_FLOOR_Z
  val DefaultCeilZ = Sector.DEFAULT_CEILING_Z

  /** probably just for testing */
  def apply(floorTex: Int, ceilTex: Int): SectorPrefab = SectorPrefab(
    DefaultFloorZ,
    floorTex,
    DefaultCeilZ,
    ceilTex
  )
}

case class SectorPrefab(
  floorZ: Int,
  floorTex: Int,
  ceilZ: Int,
  ceilTex: Int
) {
  def writeTo(s: Sector): Unit ={
    s.setFloorZ(floorZ)
    s.setFloorTexture(floorTex)
    s.setCeilingZ(ceilZ)
    s.setCeilingTexture(ceilTex)
  }
}

/**
  * Here "wall" refers to the entire side of a room, which may itself contain multiple Build walls.
  */
case class LoungeWall(
  heading: Int,
  points: Seq[PointXY],
  wallTypes: Seq[String]
){
  require(Heading.all.contains(heading))
  require(points.nonEmpty)
}

case class LoungeControlPoints(
  east: LoungeWall,
  south: LoungeWall,
  west: LoungeWall,
  north: LoungeWall
) {
  val all = Seq(east, south, west, north)

  def allPoints = all.flatMap(_.points)
  def allWallTypes = all.flatMap(_.wallTypes)
  def allEastPoints: Seq[PointXY] = east.points :+ south.points(0)
  def allSouthPoints: Seq[PointXY] = south.points :+ west.points(0)
  def allWestPoints: Seq[PointXY] = west.points :+ north.points(0)
  def allNorthPoints: Seq[PointXY] = north.points :+ east.points(0)

  def allEastSections: Seq[String] = east.wallTypes :+ south.wallTypes(0)
  def allSouthSections: Seq[String] = south.wallTypes :+ west.wallTypes(0)
  def allWestSections: Seq[String] = west.wallTypes :+ north.wallTypes(0)
  def allNorthSections: Seq[String] = north.wallTypes :+ east.wallTypes(0)

  def NW: PointXY = north.points(0)
  def NE: PointXY = east.points(0)
  def SE: PointXY = south.points(0)
  def SW: PointXY = west.points(0)

  def points(heading: Int): Seq[PointXY] = {
    require(heading >= 0 && heading < 4)
    // the other corner is always the beginning of the next wall
    all(heading).points :+ all((heading+1) % 4).points(0)
  }
  def wallTypes(heading: Int): Seq[String] = {
    require(heading >= 0 && heading < 4)
    // the other corner is always the beginning of the next wall
    all(heading).wallTypes :+ all((heading+1) % 4).wallTypes(0)
  }

  def sections(heading: Int): Seq[(PointXY, String)] = points(heading).zip(wallTypes(heading))

}

object LoungeWall {
  val Empty = "EMPTY"
  val Anchor = "ANCHOR"
}


object LoungePrinter {

  private def toI(f: FVectorXY): PointXY = new PointXY(f.x.toInt, f.y.toInt)

  private def compassWall(anchor: WallAnchor): Int = MapUtil.compassWallSide(toI(anchor.vector.toF.normalized()))

  // TODO i want the option to print to the same Map, or to a new Sector Group (which has a self-contained map object)
  // pasting to a sector group and copying in is good for things like activator lotags

  /*

case class StairEntrance(p0: PointXY, p1: PointXY, floorZ: Int, ceilZ: Int, sectorId: Option[Int]) {
   */

  /**
    * Calculates the control points by just constructing a bounding box with the anchor points.
    *
    * NW     A0     A1    NE/B0
    *
    *                     B1
    *
    *
    *
    * SW                  SE
    *
    */
  private[moonbase2] def controlPointsSimple(
    wallA: WallAnchor,
    wallB: WallAnchor
  ): LoungeControlPoints = {
    require(wallA.axisAligned && wallB.axisAligned)
    require(compassWall(wallA) != compassWall(wallB))

    // TODO actually it would be ok for them to share a corner...
    require(! (wallA.p0.equals(wallB.p1) || wallA.p1.equals(wallB.p0)))

    val bb = BoundingBox.apply(wallA.points ++ wallB.points)
    require(bb.area > 0)

    // val northAnchor = Seq(wallA, wallB).find(compassWall(_) == Heading.N)
    val anchors: Map[Int, Option[WallAnchor]] = Heading.all.asScala.map(h => h.toInt -> Seq(wallA, wallB).find(compassWall(_) == h)).toMap

    // This is what sideControlPoints does:
    // val northPoints = mutable.ArrayBuffer[PointXY]()
    // northPoints.append(bb.topLeft)
    // northAnchor.foreach{ anchor =>
    //   if(anchor.p0 == bb.topLeft){
    //     northPoints.append(anchor.p1)
    //   }else if(anchor.p1 == bb.topRight){
    //     northPoints.append(anchor.p0)
    //   }else{
    //     northPoints.append(anchor.p0)
    //     northPoints.append(anchor.p1)
    //   }
    // northPoints.append(bb.topRight)
    // }
    def sideControlPoints(heading: Int, corner0: PointXY, corner1: PointXY, anchorOpt: Option[WallAnchor]): LoungeWall = {
      val points = mutable.ArrayBuffer[PointXY]()
      val sections = mutable.ArrayBuffer[String]()
      points.append(corner0)
      sections.append(LoungeWall.Empty)
      anchorOpt.foreach { anchor =>
        if(anchor.p0 == corner0 && anchor.p1 == corner1){
          // do nothing; we already added corner0 and p1/corner one gets added in the next side
          sections.update(0, LoungeWall.Anchor)
        }else if(anchor.p0 == corner0){
          points.append(anchor.p1)
          sections.update(0, LoungeWall.Anchor)
          sections.append(LoungeWall.Empty)
        }else if(anchor.p1 == corner1){
          points.append(anchor.p0)
          sections.append(LoungeWall.Anchor)
        }else{
          points.append(anchor.p0)
          points.append(anchor.p1)
          sections.append(LoungeWall.Anchor)
          sections.append(LoungeWall.Empty)
        }
      }
      // points.append(corner1) leave this corner for the next edge?
      LoungeWall(heading, points, sections)
    }

    val north = sideControlPoints(Heading.N, bb.topLeft, bb.topRight, anchors(Heading.N))
    val east = sideControlPoints(Heading.E, bb.topRight, bb.bottomRight, anchors(Heading.E))
    val south = sideControlPoints(Heading.S, bb.bottomRight, bb.bottomLeft, anchors(Heading.S))
    val west = sideControlPoints(Heading.W, bb.bottomLeft, bb.topLeft, anchors(Heading.W))
    //Seq(north, east, south, west)
    LoungeControlPoints(east, south, west, north)
  }


  /**
    * Creates a "Lounge Hallway" between sectors A and B, positioned to match the walls (entApoint0->entAPoint1) and
    * (entBpoint0->entBpoint1).
    *
    *      |   B   |
    *     p1<------p0
    *          |
    *         \/
    *
    *
    *          /\
    *          |
    *     p0----->p1
    *      |   A  |
    *
    *
    * @param entBfloorZ floor Z of room B
    * @param entBceilZ ceiling Z of room B
    */
  def printLounge(
    gameCfg: GameConfig,
    map: DMap,
    wallA: WallAnchor,
    // entAfloorZ: Int, // TODO floor and ceil can come from the anchor objects
    // entAceilZ: Int,

    wallB: WallAnchor,
    //point0B: PointXY,
    //point1B: PointXY,
    entBfloorZ: Int,
    entBceilZ: Int,
  ): Unit = {
    require(wallA.axisAligned && wallB.axisAligned)

    val loungeZ: Int = (wallA.floorZ + entBfloorZ) / 2
    val loungeCeilZ: Int = loungeZ - (32 * BuildConstants.ZStepHeight)

    def entranceLength(verticalDrop: Int): Int = if (verticalDrop > SpacePassagePrinter.MinElevatorDrop) {
        1024 + 16 // 1024+ is min length for elevator
      }else if(verticalDrop > 1024 * 6){
        768
      }else if(verticalDrop > 1024 * 3){
        512
      }else{
        256
    }

    // TODO draw this all in a separate map and make a sector group out of it?
    // val entranceWall = gameCfg.tex(258)
    // val entrancePrefab = SectorPrefab(183, 181)

    // val normalA = wallA.p0.vectorTo(wallA.p1).toF.rotatedCW.normalized.multipliedBy(entranceLength(Math.abs(wallA.floorZ - loungeZ))).toPointXY
    val normalA = wallA.vector.toF.rotatedCW.normalized.multipliedBy(entranceLength(Math.abs(wallA.floorZ - loungeZ))).toPointXY
    println(normalA)
    val newWallA = WallAnchor(wallA.p0.add(normalA), wallA.p1.add(normalA), wallA.floorZ, loungeCeilZ)
    val entranceAResult = SpacePassagePrinter.printSpacePassage(gameCfg, map, wallA, newWallA.reversed)

    val normalB = wallB.p0.vectorTo(wallB.p1).toF.rotatedCW.normalized.multipliedBy(entranceLength(Math.abs(entBfloorZ - loungeZ))).toPointXY
    //val (newPoint0B, newPoint1B) = drawEntrance(map, point0B, point1B, normalB, entranceLength, entranceWall, entrancePrefab)
    val newWallB = WallAnchor(wallB.p0.add(normalB), wallB.p1.add(normalB), wallA.floorZ, loungeCeilZ)
    val entranceBResult = SpacePassagePrinter.printSpacePassage(gameCfg, map, wallB, newWallB.reversed)

    println(s"newWallA=${newWallA} newWallB=${newWallB}")
    val controlPoints = controlPointsSimple(
      newWallA, newWallB
    )
    val points = controlPoints.allPoints
    val sections = controlPoints.allWallTypes

    val innerFloor = HorizontalBrush(898).withRelative(true).withSmaller(true).withShade(15)
    val edgeFloor = HorizontalBrush(183).withShade(15).withSmaller(true)
    val ceiling = HorizontalBrush(182).withShade(15)

    val outerNW = controlPoints.NW
    val outerNE = controlPoints.NE
    val outerSE = controlPoints.SE
    val outerSW = controlPoints.SW

    val innerNW = outerNW.add(new PointXY(512, 512))
    val innerNE = outerNE.add(new PointXY(-512, 512))
    val innerSE = outerSE.add(new PointXY(-512, -512))
    val innerSW = outerSW.add(new PointXY(512, -512))

    def sectionList(heading: Int) = {
      // i make the control points class return the wallType of the wall on the next side of the room, but we dont
      // want that here because the next wall is actually a red wall
      val list = controlPoints.sections(heading)
      val last = list.last
      list.dropRight(1) :+ (last._1, "RW")
    }

    val eSectorId = edgeSector(gameCfg, map, edgeFloor, ceiling, loungeZ, loungeCeilZ, sectionList(Heading.E) ++ Seq((innerSE, "RW"), (innerNE, "RW")))
    val sSectorId = edgeSector(gameCfg, map, edgeFloor, ceiling, loungeZ, loungeCeilZ, sectionList(Heading.S) ++ Seq((innerSW, "RW"), (innerSE, "RW")))
    val wSectorId = edgeSector(gameCfg, map, edgeFloor, ceiling, loungeZ, loungeCeilZ, sectionList(Heading.W) ++ Seq((innerNW, "RW"), (innerSW, "RW")))
    val nSectorId = edgeSector(gameCfg, map, edgeFloor, ceiling, loungeZ, loungeCeilZ, sectionList(Heading.N) ++ Seq((innerNE, "RW"), (innerNW, "RW")))

    val innerSectorId = printInnerSector(gameCfg, map, innerSE, innerSW, innerNW, innerNE, loungeZ, loungeCeilZ, innerFloor, ceiling)

    val sectorIds = Seq(innerSectorId, eSectorId, sSectorId, wSectorId, nSectorId, entranceAResult._2.sectorId, entranceBResult._2.sectorId)
    sectorIds.foreach { id0 =>
      sectorIds.foreach { id1 =>
        MiscPrinter.autoLinkRedWalls(map, id0, id1)
      }
    }

  }


  def fillWallSection(gameCfg: GameConfig, map: DMap, p0: PointXY, p1: PointXY, floorZ: Int, ceilZ: Int, loungeWall: WallPrefab, loungeCeil: HorizontalBrush): (Seq[Int], Seq[Wall], PointXY) = {

    /** return  (list of new sector ids, walls for main loop, "new p0" point) */
    def chairs(gameCfg: GameConfig, map: DMap, p0: PointXY, p1: PointXY, chairCount: Int): (Seq[Int], Seq[Wall], PointXY) = {

      val SeatBackHeight = 7 * BuildConstants.ZStepHeight
      val SeatHeight = 4 * BuildConstants.ZStepHeight
      val ChairBackSide = WallPrefab(gameCfg.tex(786)).copy(xrepeat=Some(4), yrepeat=Some(8)).withShade(15)
      val ChairSeatSide = WallPrefab(gameCfg.tex(788)).copy(xrepeat=Some(4), yrepeat=Some(16)).withShade(15) // TODO need to adjust xrepeat based on wall length
      val ChairSeatFloor = HorizontalBrush(gameCfg.tex(786)).withRelative(true).withSmaller(true)
      val chairFrontXRepeat = ChairSeatSide.tex.get.xRepeatForNRepetitions(chairCount)     //c2.distanceTo(c3).toInt

      val _ :: c1 :: c2 :: c3 :: c4 :: c5 :: Nil = LoungeWallPrinter.chairControlPoints(p0, p1, chairCount)
      val seatBackTex = WallPrefab(gameCfg.tex(786)).withShade(15).copy(xrepeat = Some(24), yrepeat = Some(8))

      // TODO in addition to xRepeatForScale, also need ot calculate an offset.  Maybe its best to use do a mod on its global position (set the offset so the texture always begins at x=0)
      val seatBackId = MiscPrinter.createSector(map, Seq(wall(p0, loungeWall.withXRepeatForScale(1.0, p0.distanceTo(c5).toInt)), wall(c5, WallPrefab.Empty), wall(c4, WallPrefab.Empty), wall(c1, WallPrefab.Empty)), floorZ - SeatBackHeight, ceilZ)
      ChairSeatFloor.writeToFloor(map.getSector(seatBackId))
      loungeCeil.writeToCeil(map.getSector(seatBackId))
      val seatId = MiscPrinter.createSector(map, Seq(c1, c4, c3, c2).map(MiscPrinter.wall(_, seatBackTex.copy(xrepeat=Some(chairFrontXRepeat)))), floorZ - SeatHeight, ceilZ)
      ChairSeatFloor.writeToFloor(map.getSector(seatId))
      loungeCeil.writeToCeil(map.getSector(seatId))
      MiscPrinter.autoLinkRedWalls(map, seatBackId, seatId)


      val outerWalls = Seq(
        wall(p0, ChairBackSide), wall(c1, ChairSeatSide), wall(c2, ChairSeatSide.copy(xrepeat=Some(chairFrontXRepeat))), wall(c3, ChairSeatSide), wall(c4, ChairBackSide)
      )

      // (Seq(seatBackId, seatId), Seq(p0, c1, c2, c3, c4).map(MiscPrinter.wall(_, loungeWall)), c5)
      (Seq(seatBackId, seatId), outerWalls, c5)

    }

    val parts = LoungePlanner2.planWall(p0.manhattanDistanceTo(p1))

    val sectorIds = mutable.ArrayBuffer[Int]()
    val outsideWalls = mutable.ArrayBuffer[Wall]()
    var cursor: PointXY = p0
    parts.foreach { part =>
      val (newSectorIds, newOutsideWalls, newP0) = part match {
        case LoungePlanner2.S => {
          LoungeWallPrinter.emptyWall(cursor, p1, 512, loungeWall)
        }
        case LoungePlanner2.C2 => {
          chairs(gameCfg, map, cursor, p1, 2)
        }
        case LoungePlanner2.C4 => {
          chairs(gameCfg, map, cursor, p1, 4)
        }
        case LoungePlanner2.WI => {
          // val (newSectorIds, newOutSideWalls, newP0) = LoungeWallPrinter.medCabinet(gameCfg, map, cursor, p1, floorZ, loungeWall)
          // LoungeWallPrinter.powerCabinet(gameCfg, map, cursor, p1, 28, floorZ, loungeWall) // 28 == shotgun
          LoungeWallPrinter.waterFountain(gameCfg, map, cursor, p1, floorZ, loungeWall)
          // LoungeWallPrinter.securityScreen(gameCfg, map, cursor, p1,floorZ, loungeWall)
          // TODO: tripbomb placement
          // TODO: window
        }
        case _ => throw new Exception(s"part ${part} not implemented yet")
      }
      sectorIds ++= newSectorIds
      outsideWalls ++= newOutsideWalls
      cursor = newP0

    }

    // val (newSectorIds, newOutsideWalls, newP0) = chairs(gameCfg, map, p0, p1)

    //val outsideWalls = newOutsideWalls :+ lastWall
    //val outsideWalls = Seq(p0, c1, c2, c3, c4, c5).map(MiscPrinter.wall(_, something))

    // (Seq(seatBackId, seatId), outsideWalls)
    val lastWall = wall(cursor, loungeWall)
    (sectorIds, outsideWalls :+ lastWall, cursor)
  }


  /**
    * so this thing is going to create some sectors (returning their ids) and then return SOME OF the walls in the main loop
    * of the "edge" area.
    */
  def wallSection(gameCfg: GameConfig, map: DMap, p0: PointXY, p1: PointXY, wallType: String, floorZ: Int, ceilZ: Int, loungeCeil: HorizontalBrush): (Seq[Int], Seq[Wall]) = {
    val loungeWall = WallPrefab(gameCfg.tex(215)).copy(yrepeat = Some(8), shade = Some(15))

    if(wallType == LoungeWall.Empty){

      // val pp = new LineSegmentXY(p0, p1).midpoint()
      // Seq(
      //   MiscPrinter.wall(p0, loungeWall.withShade(22)),
      //   MiscPrinter.wall(pp, loungeWall)
      // )


      val (sectorIds, walls, newP0) = fillWallSection(gameCfg, map, p0, p1, floorZ, ceilZ, loungeWall, loungeCeil: HorizontalBrush)
      val lastWall = MiscPrinter.wall(newP0, loungeWall) // TODO do i need this?
      // TODO: join the sector ids
      // (sectorIds, Seq(MiscPrinter.wall(p0, loungeWall)) ++ walls)
      (sectorIds, walls)

    }else if(wallType == LoungeWall.Anchor){
      (Seq.empty, Seq(MiscPrinter.wall(p0, loungeWall.copy(alignBottom = Some(true)))))
    }else{
      (Seq.empty, Seq(MiscPrinter.wall(p0, WallPrefab.Empty)))
    }

  }

  def edgeSector(gameCfg: GameConfig, map: DMap, edgeFloor: HorizontalBrush, ceiling: HorizontalBrush, loungeZ: Int, loungeCeilZ: Int, sections: Seq[(PointXY, String)]): Int = {

    val (points, wallTypes) = sections.unzip
    val withNextPoints = MiscPrinter.withP2(points).zip(wallTypes)

    val wallSectorIds = mutable.ArrayBuffer[Int]()
    val loop = withNextPoints.flatMap { case ((p, nextPoint), wallType) =>
      val (sectorIds, loop) = wallSection(gameCfg, map, p, nextPoint, wallType, loungeZ, loungeCeilZ, ceiling)
      wallSectorIds.append(sectorIds: _*)
      loop
    }

    val sectorId = MiscPrinter.createSector(map, loop, loungeZ, loungeCeilZ)
    MiscPrinter.autoLinkRedWalls(map, wallSectorIds :+ sectorId)

    edgeFloor.writeToFloor(map.getSector(sectorId))
    ceiling.writeToCeil(map.getSector(sectorId))
    sectorId
  }

  def printInnerSector(gameCfg: GameConfig, map: DMap, innerSE: PointXY, innerSW: PointXY, innerNW: PointXY, innerNE: PointXY, loungeZ: Int, loungeCeilZ: Int, innerFloor: HorizontalBrush, ceiling: HorizontalBrush): Int = {

    val xSize = Math.abs(innerSE.x - innerSW.x)
    val ySize = Math.abs(innerSE.y - innerNE.y)

    // with at least this much distance, we can have a kiosk at either end of the room.  Any less, and we just put
    // one in the middle
    val KioskFootPrint = 1536 // amount of space kiosk needs, including margin for people to walk
    val MinDist2Kiosks = 3840
    val kioskCenterX: Seq[Int] = if(xSize < KioskFootPrint){
      Seq()
    }else if(xSize < MinDist2Kiosks){
      Seq((innerSE.x + innerSW.x) / 2)
    }else{
      Seq(innerSW.x + 768, innerSE.x - 768)
    }
    val kioskCenterY: Seq[Int] = if(ySize < 1536) {
      Seq()
    }else if(ySize < MinDist2Kiosks) {
      Seq((innerNE.y + innerSE.y) / 2)
    }else{
      Seq(innerNE.y + 768, innerSE.y - 768)
    }
    val kiosks = kioskCenterX.flatMap(x => kioskCenterY.map(y => (x, y)))
    val kioskResults = kiosks.map{ case(centerx, centery) =>
      printKiosk(gameCfg, map, centerx, centery, loungeZ, loungeCeilZ, innerFloor.withShade(0).withSmaller(true))
    }

    val outerWalls = Seq(innerSE, innerSW, innerNW, innerNE).map(p => MiscPrinter.wall(p, WallPrefab.Empty))

    // these pop down from the ceiling
    val KioskOuterWall = WallPrefab(gameCfg.tex(182)).withShade(15)
    val moreWalls = kioskResults.map(r => r._2.map(p => MiscPrinter.wall(p, KioskOuterWall)).reverse)
    val innerSectorId = MiscPrinter.createMultiLoopSector(map, outerWalls +: moreWalls, loungeZ, loungeCeilZ)
    innerFloor.writeToFloor(map.getSector(innerSectorId))
    ceiling.writeToCeil(map.getSector(innerSectorId))

    kioskResults.map(r => MiscPrinter.autoLinkRedWalls(map, innerSectorId, r._1))
    innerSectorId
  }

  def printKiosk(gameCfg: GameConfig, map: DMap, centerx: Int, centery: Int, loungeZ: Int, loungeCeilZ: Int, kioskFloor: HorizontalBrush): (Int, Seq[PointXY]) = {
    val KioskWidth = 768 // width of the outer kios sector(s).
    def box(cx: Int, cy: Int, hw: Int) = Seq(new PointXY(cx - hw, cy - hw), new PointXY(cx + hw, cy - hw), new PointXY(cx + hw, cy + hw), new PointXY(cx - hw, cy + hw))

    // val innerWall = WallPrefab(Some(gameCfg.tex(297)), Some(7), None, Some(8), Some(16), None, None, None, None)
    val innerWall = WallPrefab(gameCfg.tex(297)).withShade(7).copy(xrepeat=Some(8), yrepeat=Some(16))
    val innerLoop = box(centerx, centery, 256).reverse.map(p => MiscPrinter.wall(p, innerWall))
    val kioskCeil = HorizontalBrush(708).withShade(7).withSmaller(true)

    val outerLoop = box(centerx, centery, KioskWidth / 2)
    val kioskSectorId = MiscPrinter.createMultiLoopSector(
      map,
      Seq(outerLoop.map(p => MiscPrinter.wall(p, WallPrefab.Empty)), innerLoop),
      loungeZ,
      loungeZ - (BuildConstants.ZStepHeight * 24)
    )
    val kioskSector = map.getSector(kioskSectorId)
    kioskFloor.writeToFloor(kioskSector)
    kioskCeil.writeToCeil(kioskSector)

    (kioskSectorId, outerLoop)
  }

  private def drawMainRoom(
    gameCfg: GameConfig,
    map: DMap,
    point0A: PointXY,
    point1A: PointXY,
    entAfloorZ: Int,
    entAceilZ: Int,
    point0B: PointXY,
    point1B: PointXY,
    entBfloorZ: Int,
    entBceilZ: Int,
  ): Unit = {
    BoundingBox(Seq(point0A, point1A, point0B, point1B))

  }


  /** for testing */
  def main(args: Array[String]): Unit = {
    //testReadForceField()
    //testGetSectorZ()

    // TODO: make sure we use something like this from StairPrinter
    // snapToNearest(floorZs(i), BuildConstants.ZStepHeight),

    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    val writer = MapWriter(gameCfg)

    test2(gameCfg, writer.getMap)
    // test(gameCfg, writer.getMap)

    writer.disarmAllSkyTextures()
    // writer.setAnyPlayerStart(force = true)
    writer.getMap.setPlayerStart(new PlayerStart(512, 512, 0, PlayerStart.NORTH))
    writer.sgBuilder.clearMarkers()
    writer.checkSectorCount()
    Main.deployTest(writer.outMap, "output.map", HardcodedConfig.getEduke32Path("output.map"))
  }

  def testReadForceField(): Unit = {
    val map = new MapLoader(HardcodedConfig.EDUKE32PATH).load("test.map")
    (0 until map.getWallCount).map(map.getWall).filter(_.isRedWall).foreach { w =>
      println(w)
    }

  }

  def testGetSectorZ(): Unit = {
    val map = new MapLoader(HardcodedConfig.EDUKE32PATH).load("test.map")

    (0 until map.getSectorCount).map { sectorId =>

      println(s"\nSector ${sectorId}")
      println(s"Floor picnum: ${map.getSector(sectorId).getFloorTexture}") // water is 336, middle is 183, slime is 200
      println(s"Floor Z: ${map.getSector(sectorId).getFloorZ}")
      println(s"Floor Slope: ${map.getSector(sectorId).getFloorSlope()}")
    }
    // one slope up = -512 (remember y+ goes down)
    // two slope up:  -1024
    // three slope up:  -1536
    // 4[ -> -2048, 5[ = -2560
    // 6[ = -3072
    // 7[ =  -3584
    // 8[ = -4096 // 45 degrees
    // 9[ = -4608
    // 10[ = -5120
    // 11[ = -5632
    // 12[ = -6144
    // 13[ = -6656
    // 14[ = -7168
    // 15[ = -7680
    // 16[ = -8192
    // 17[ = -8704
    // 18[ = -9216

    // 26[ = -13312
    // 64[ = -32767 // this is the highest value allowed

    // Sloping DOWN:
    // 1] = 512
    // 2] = 1024
    // 4] = 2048
    // 8] = 4096
    // 16] = 8192
    // 32] = 16384
    // 64] = 32767


  }

  def test2(gameCfg: GameConfig, map: DMap): Unit = {

    val wallA = WallAnchor(new PointXY(0, 2048), new PointXY(0, 0), BuildConstants.DefaultFloorZ, BuildConstants.DefaultCeilZ)
    val wallB = WallAnchor(new PointXY(4096, -2048), new PointXY(4096+1024, -2048), BuildConstants.DefaultFloorZ, BuildConstants.DefaultCeilZ)
    printLounge(gameCfg, map: DMap,
      wallA: WallAnchor,
      wallB: WallAnchor,
      BuildConstants.DefaultFloorZ,
      BuildConstants.DefaultCeilZ)
  }



}
