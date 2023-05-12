package trn.bespoke.moonbase2

import trn.{BuildConstants, PointXY, MapUtil, RandomX, Wall, HardcodedConfig, ScalaMapLoader, FVectorXY, Main, PlayerStart, Map => DMap}
import trn.prefab.{BoundingBox, MapWriter, DukeConfig, GameConfig, Heading}
import trn.render.{MiscPrinter, WallPrefab, WallAnchor, HorizontalBrush, ResultAnchor}
import trn.render.MiscPrinter.wall

import scala.collection.JavaConverters._
import scala.collection.mutable


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

  def canPrintLounge(
    wallA: WallAnchor,
    wallB: WallAnchor,
  ): Boolean = {
    // TODO need to make sure they are facing each other.  And this distance test is nearly worthless (I'm being lazy right now)
    wallA.axisAligned && wallB.axisAligned && wallA.p0.distanceTo(wallB.p0) > 1024 * 4
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
    */
  def printLounge(
    r: RandomX,
    gameCfg: GameConfig,
    map: DMap,
    wallA: WallAnchor,
    wallB: WallAnchor,
  ): (ResultAnchor, ResultAnchor) = {
    require(canPrintLounge(wallA, wallB))
    // require(wallA.axisAligned && wallB.axisAligned)

    val loungeZ: Int = (wallA.floorZ + wallB.floorZ) / 2
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

    val normalA = wallA.vector.toF.rotatedCW.normalized.multipliedBy(entranceLength(Math.abs(wallA.floorZ - loungeZ))).toPointXY
    println(normalA)
    val newWallA = WallAnchor(wallA.p0.add(normalA), wallA.p1.add(normalA), wallA.floorZ, loungeCeilZ)
    val entranceAResult = SpacePassagePrinter.printSpacePassage(gameCfg, map, wallA, newWallA.reversed)

    val normalB = wallB.p0.vectorTo(wallB.p1).toF.rotatedCW.normalized.multipliedBy(entranceLength(Math.abs(wallB.floorZ - loungeZ))).toPointXY
    val newWallB = WallAnchor(wallB.p0.add(normalB), wallB.p1.add(normalB), wallA.floorZ, loungeCeilZ)
    val entranceBResult = SpacePassagePrinter.printSpacePassage(gameCfg, map, wallB, newWallB.reversed)

    // println(s"newWallA=${newWallA} newWallB=${newWallB}")
    val controlPoints = controlPointsSimple(
      newWallA, newWallB
    )
    // val points = controlPoints.allPoints
    // val sections = controlPoints.allWallTypes

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

    val eSectorId = edgeSector(r, gameCfg, map, edgeFloor, ceiling, loungeZ, loungeCeilZ, sectionList(Heading.E) ++ Seq((innerSE, "RW"), (innerNE, "RW")))
    val sSectorId = edgeSector(r, gameCfg, map, edgeFloor, ceiling, loungeZ, loungeCeilZ, sectionList(Heading.S) ++ Seq((innerSW, "RW"), (innerSE, "RW")))
    val wSectorId = edgeSector(r, gameCfg, map, edgeFloor, ceiling, loungeZ, loungeCeilZ, sectionList(Heading.W) ++ Seq((innerNW, "RW"), (innerSW, "RW")))
    val nSectorId = edgeSector(r, gameCfg, map, edgeFloor, ceiling, loungeZ, loungeCeilZ, sectionList(Heading.N) ++ Seq((innerNE, "RW"), (innerNW, "RW")))

    val innerSectorId = printInnerSector(gameCfg, map, innerSE, innerSW, innerNW, innerNE, loungeZ, loungeCeilZ, innerFloor, ceiling)

    val sectorIds = Seq(innerSectorId, eSectorId, sSectorId, wSectorId, nSectorId, entranceAResult._2.sectorId, entranceBResult._2.sectorId)
    sectorIds.foreach { id0 =>
      sectorIds.foreach { id1 =>
        MiscPrinter.autoLinkRedWalls(map, id0, id1)
      }
    }

    (entranceAResult._1, entranceBResult._1)
  }


  def fillWallSection(r: RandomX, gameCfg: GameConfig, map: DMap, p0: PointXY, p1: PointXY, floorZ: Int, ceilZ: Int, loungeWall: WallPrefab, loungeFloor: HorizontalBrush, loungeCeil: HorizontalBrush): (Seq[Int], Seq[Wall]) = {

    // Did this for testing:
    // val parts = LoungePlanner2.planWallOld(p0.manhattanDistanceTo(p1).toInt)

    val TODO: Set[String] = Set.empty
    val totalLength = p0.manhattanDistanceTo(p1).toInt
    val parts = LoungePlanner2.planWall(totalLength, TODO, r)
    require(parts.head.length >= 512)
    require(parts.head.length >= 512 && parts.last.length >= 512)
    require(parts.map(_.length).sum <= totalLength)

    val sectorIds = mutable.ArrayBuffer[Int]()
    val outsideWalls = mutable.ArrayBuffer[Wall]()
    var cursor: PointXY = p0
    val outerSector = OuterSector(floorZ, ceilZ, loungeFloor, loungeCeil, loungeWall)
    parts.foreach { part =>
      val (newSectorIds, newOutsideWalls, newP0) = part match {
        case Item(LoungePlanner2.S.name, dist) => {
          LoungeWallPrinter.emptyWall(cursor, p1, dist, loungeWall)
        }
        case LoungePlanner2.C2 => LoungeWallPrinter.chairs(gameCfg, map, cursor, p1, outerSector, 2)
        case LoungePlanner2.C3 => { LoungeWallPrinter.chairs(gameCfg, map, cursor, p1, outerSector, 3) }
        case LoungePlanner2.C4 => { LoungeWallPrinter.chairs(gameCfg, map, cursor, p1, outerSector, 4) }
        case LoungePlanner2.C5 => { LoungeWallPrinter.chairs(gameCfg, map, cursor, p1, outerSector, 5) }
        case LoungePlanner2.C6 => { LoungeWallPrinter.chairs(gameCfg, map, cursor, p1, outerSector, 6) }
        case LoungePlanner2.T => {
          LoungeWallPrinter.table(r, gameCfg, map, cursor, p1, floorZ, ceilZ, loungeWall, loungeCeil)
        }
        case LoungePlanner2.WI => {
          // val (newSectorIds, newOutSideWalls, newP0) = LoungeWallPrinter.medCabinet(gameCfg, map, cursor, p1, floorZ, loungeWall)
          // LoungeWallPrinter.powerCabinet(gameCfg, map, cursor, p1, 28, floorZ, loungeWall) // 28 == shotgun
          LoungeWallPrinter.waterFountain(gameCfg, map, cursor, p1, outerSector)
          // LoungeWallPrinter.securityScreen(gameCfg, map, cursor, p1,floorZ, loungeWall)
          // TODO: window
          // TODO (and then the table...maybe centerpiece ideas, etc)
          // - decal like a painting
          // wall of lights floor to ceiling! (see it in E2l5 in room with battle lords)
          // TODO: cubby for space suits, kind of like end button of E2L3 but with glass in front

          // TODO: adjustable:  fans

          // later:
          // TODO: tripbomb placement
          // TODO: gun placement (classic E1 red door, long and short like in E2L1, is in red band and opens up to multiple guns
          //      - can be adjustable to number of guns
          // TODO: closet full of sentry drones?

          //TODO ideas
          // - window onto pipes?
          // - set of lockers ... like E2L7
          ???
        }
        case Item(LoungePlanner2.BulkHead, length) => LoungeWallPrinter.bulkhead(gameCfg, map, cursor, p1, loungeWall, length)
        case Item(LoungePlanner2.Window, length) => LoungeWallPrinter.window(gameCfg, map, cursor, p1, outerSector, length)
        case Item(LoungePlanner2.Medkit, length) => {
          // require(length == 768) // TODO actually these functions should be capable of taking a length
          LoungeWallPrinter.medCabinet(gameCfg, map, cursor, p1, floorZ, loungeWall)
        }
        case Item(LoungePlanner2.SecurityScreen, length) => {
          // require(length == 768) // TODO
          LoungeWallPrinter.securityScreen(gameCfg, map, cursor, p1, outerSector)
        }
        case Item(LoungePlanner2.PowerCabinet, length) => {
          // require(length == 512) // TODO
          val Shotgun = 28 // TODO
          LoungeWallPrinter.powerCabinet(gameCfg, map, cursor, p1, Shotgun, outerSector)
        }
        case Item(LoungePlanner2.Fountain, length) => {
          // require(length == 640) // TODO
          LoungeWallPrinter.waterFountain(gameCfg, map, cursor, p1, outerSector)
        }
        case Item(LoungePlanner2.Fans, length) => LoungeWallPrinter.fan(gameCfg, map, cursor, p1, outerSector, length)
        case Item(LoungePlanner2.PowerHole, length) => LoungeWallPrinter.powerHole(gameCfg, map, cursor, p1, outerSector, length, 54) // TODO 54 == Armor
        case Item(LoungePlanner2.TwoScreens, length) => LoungeWallPrinter.twoScreens(r, gameCfg, map, cursor, p1, outerSector, length)
        case Item(LoungePlanner2.EDFDecal, length) => LoungeWallPrinter.edfDecal(gameCfg, map, cursor, p1, outerSector, length)
        case Item(LoungePlanner2.SpaceSuits, length) => LoungeWallPrinter.spaceSuits(gameCfg, map, cursor, p1, outerSector, length)
        case x => {
          throw new Exception(s"part ${part} not implemented yet")
          // TODO
          // LoungeWallPrinter.emptyWall(cursor, p1, x.length, loungeWall)
        }
      }
      sectorIds ++= newSectorIds
      outsideWalls ++= newOutsideWalls
      cursor = newP0
    }

    val lastWall: Seq[Wall] = if(cursor == p1){
      Seq.empty // no need to add a last wall!
    }else{
      Seq(wall(cursor, loungeWall))
    }

    (sectorIds, outsideWalls ++ lastWall)
  }


  /**
    * so this thing is going to create some sectors (returning their ids) and then return SOME OF the walls in the main loop
    * of the "edge" area.
    */
  def wallSection(r: RandomX, gameCfg: GameConfig, map: DMap, p0: PointXY, p1: PointXY, wallType: String, floorZ: Int, ceilZ: Int, loungeCeil: HorizontalBrush, edgeFloor: HorizontalBrush): (Seq[Int], Seq[Wall]) = {
    val loungeWall = WallPrefab(gameCfg.tex(215)).copy(yrepeat = Some(8), shade = Some(15))

    if(wallType == LoungeWall.Empty){
      val (sectorIds, walls) = fillWallSection(r, gameCfg, map, p0, p1, floorZ, ceilZ, loungeWall, edgeFloor, loungeCeil: HorizontalBrush)
      (sectorIds, walls)

    }else if(wallType == LoungeWall.Anchor){
      (Seq.empty, Seq(MiscPrinter.wall(p0, loungeWall.copy(alignBottom = Some(true)))))
    }else{
      (Seq.empty, Seq(MiscPrinter.wall(p0, WallPrefab.Empty)))
    }

  }

  def edgeSector(r: RandomX, gameCfg: GameConfig, map: DMap, edgeFloor: HorizontalBrush, ceiling: HorizontalBrush, loungeZ: Int, loungeCeilZ: Int, sections: Seq[(PointXY, String)]): Int = {

    val (points, wallTypes) = sections.unzip
    val withNextPoints = MiscPrinter.withP2(points).zip(wallTypes)

    val wallSectorIds = mutable.ArrayBuffer[Int]()
    val loop = withNextPoints.flatMap { case ((p, nextPoint), wallType) =>
      val (sectorIds, loop) = wallSection(r, gameCfg, map, p, nextPoint, wallType, loungeZ, loungeCeilZ, ceiling, edgeFloor)
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


  /** for testing */
  def main(args: Array[String]): Unit = {
    //testReadForceField()
    //testGetSectorZ()

    // TODO: make sure we use something like this from StairPrinter
    // snapToNearest(floorZs(i), BuildConstants.ZStepHeight),

    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile, HardcodedConfig.getAtomicHeightsFile)
    val writer = MapWriter(gameCfg)

    test2(gameCfg, writer.getMap)

    writer.disarmAllSkyTextures()
    writer.getMap.setPlayerStart(new PlayerStart(512, 512, 0, PlayerStart.NORTH))
    writer.sgBuilder.clearMarkers()
    writer.checkSectorCount()
    Main.deployTest(writer.outMap, "output.map", HardcodedConfig.getEduke32Path("output.map"))
  }

  def testReadForceField(): Unit = {
    val map = new ScalaMapLoader(HardcodedConfig.EDUKE32PATH).load("test.map")
    (0 until map.getWallCount).map(map.getWall).filter(_.isRedWall).foreach { w =>
      println(w)
    }

  }

  def testGetSectorZ(): Unit = {
    val map = new ScalaMapLoader(HardcodedConfig.EDUKE32PATH).load("test.map")

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
    val r = RandomX() // TODO pass this in
    printLounge(r, gameCfg, map: DMap, wallA: WallAnchor, wallB: WallAnchor)
  }

}
