package trn.bespoke.moonbase2

import trn.{BuildConstants, FVectorXY, HardcodedConfig, LineSegmentXY, LineXY, Main, MapLoader, MapUtil, PlayerStart, PointXY, PointXYZ, Sector, Wall, WallView, Map => DMap}
import trn.prefab.{BoundingBox, DukeConfig, GameConfig, Heading, MapWriter, RandomX}
import trn.render.{HorizontalBrush, MiscPrinter, Texture, TextureUtil, WallAnchor, WallPrefab}

import scala.collection.JavaConverters._
import scala.collection.mutable

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

    val loungeWall = WallPrefab(gameCfg.tex(215)).copy(yrepeat = Some(8), shade = Some(15))
    //val walls = points.map(p => MiscPrinter.wall(p, loungeWall))
    val innerFloor = HorizontalBrush(183).withRelative(true).withSmaller(true).withShade(15)
    val edgeFloor = HorizontalBrush(898).withShade(15)
    val ceiling = HorizontalBrush(182).withShade(15)


    val outerNW = controlPoints.NW
    val outerNE = controlPoints.NE
    val outerSE = controlPoints.SE
    val outerSW = controlPoints.SW

    val innerNW = outerNW.add(new PointXY(512, 512))
    val innerNE = outerNE.add(new PointXY(-512, 512))
    val innerSE = outerSE.add(new PointXY(-512, -512))
    val innerSW = outerSW.add(new PointXY(512, -512))

    def wallForSection(section: String): WallPrefab = {
      if(section == LoungeWall.Empty){
        loungeWall
      }else if(section == LoungeWall.Anchor){
        loungeWall.copy(alignBottom = Some(true))
      }else{
        WallPrefab(gameCfg.tex(0))
      }
    }


    def edgeSector(sections: Seq[(PointXY, String)]): Int = {
      val loop = sections.map { case(p, s) => MiscPrinter.wall(p, wallForSection(s))}
      val sectorId = MiscPrinter.createSector(map, loop, loungeZ, loungeCeilZ)
      edgeFloor.writeToFloor(map.getSector(sectorId))
      ceiling.writeToCeil(map.getSector(sectorId))
      sectorId
    }

    val eSectorId = edgeSector(controlPoints.sections(Heading.E) ++ Seq((innerSE, "RW"), (innerNE, "RW")))
    val sSectorId = edgeSector(controlPoints.sections(Heading.S) ++ Seq((innerSW, "RW"), (innerSE, "RW")))
    val wSectorId = edgeSector(controlPoints.sections(Heading.W) ++ Seq((innerNW, "RW"), (innerSW, "RW")))
    val nSectorId = edgeSector(controlPoints.sections(Heading.N) ++ Seq((innerNE, "RW"), (innerNW, "RW")))


    val innerSectorId = printInnerSector(gameCfg, map, innerSE, innerSW, innerNW, innerNE, loungeZ, loungeCeilZ, innerFloor, ceiling)
    // val innerWalls = Seq(innerSE, innerSW, innerNW, innerNE).map(p => MiscPrinter.wall(p, WallPrefab.Empty))
    // val innerSectorId = MiscPrinter.createSector(map, innerWalls, loungeZ, loungeCeilZ)
    // innerFloor.writeToFloor(map.getSector(innerSectorId))
    // ceiling.writeToCeil(map.getSector(innerSectorId))

    val sectorIds = Seq(innerSectorId, eSectorId, sSectorId, wSectorId, nSectorId, entranceAResult._2.sectorId, entranceBResult._2.sectorId)
    sectorIds.foreach { id0 =>
      sectorIds.foreach { id1 =>
        MiscPrinter.autoLinkRedWalls(map, id0, id1)
      }
    }


    // val eloop = e.map { case(p, section) => MiscPrinter.wall(p, wallForSection(section))}
    // val eSectorId = MiscPrinter.createSector(map, eloop, loungeZ, loungeCeilZ)

    // val northPattern: Seq[(PointXY, String)] = sides(0).controlPoints.zip(sides(0).sections) ++ Seq((innerNE, "RW"), (innerNW, "RW"), (outerNW, "RW"))
    // val northLoop: Seq[Wall] = northPattern.map { case (p, section) =>
    //   MiscPrinter.wall(p, wallForSection(section))
    // }
    // val northSectorId = MiscPrinter.createSector(map, northLoop, loungeZ, loungeCeilZ)

    //

    // // drawing the entire room at once
    // val walls = points.zip(sections).map { case(p,section) =>
    //   if (section == LoungeWall.Empty) {
    //     MiscPrinter.wall(p, loungeWall)
    //   } else {
    //     val w = loungeWall.copy(alignBottom = Some(true))
    //     MiscPrinter.wall(p, w)
    //   }
    // }
    // val sectorId = MiscPrinter.createSector(map, walls, loungeZ, loungeCeilZ)
    // map.getSector(sectorId).setFloorTexture(898)
    // map.getSector(sectorId).setCeilingTexture(182)
    // Seq(entranceAResult._1.sectorId, entranceAResult._2.sectorId, entranceBResult._1.sectorId, entranceBResult._2.sectorId).foreach { entranceId =>
    //   MiscPrinter.autoLinkRedWalls(map, sectorId, entranceId)
    // }

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
      printKiosk(gameCfg, map, centerx, centery, loungeZ, loungeCeilZ, HorizontalBrush(183).withShade(0).withSmaller(true))
    }


    val innerWalls = Seq(innerSE, innerSW, innerNW, innerNE).map(p => MiscPrinter.wall(p, WallPrefab.Empty))
    // val innerSectorId = MiscPrinter.createSector(map, innerWalls, loungeZ, loungeCeilZ)

    // these pop down from the ceiling
    val KioskOuterWall = WallPrefab(gameCfg.tex(182)).withShade(15)

    val moreWalls = kioskResults.map(r => r._2.map(p => MiscPrinter.wall(p, KioskOuterWall)).reverse)
    val innerSectorId = MiscPrinter.createMultiLoopSector(map, innerWalls +: moreWalls, loungeZ, loungeCeilZ)
    innerFloor.writeToFloor(map.getSector(innerSectorId))
    ceiling.writeToCeil(map.getSector(innerSectorId))

    kioskResults.map(r => MiscPrinter.autoLinkRedWalls(map, innerSectorId, r._1))
    innerSectorId
  }

  def printKiosk(gameCfg: GameConfig, map: DMap, centerx: Int, centery: Int, loungeZ: Int, loungeCeilZ: Int, kioskFloor: HorizontalBrush): (Int, Seq[PointXY]) = {
    val KioskWidth = 768 // width of the outer kios sector(s).
    def box(cx: Int, cy: Int, hw: Int) = Seq(new PointXY(cx - hw, cy - hw), new PointXY(cx + hw, cy - hw), new PointXY(cx + hw, cy + hw), new PointXY(cx - hw, cy + hw))

    // val w = centerx - KioskWidth / 2
    // val e = centerx + KioskWidth / 2
    // val n = centery - KioskWidth / 2
    // val s = centery + KioskWidth / 2
    // val loop = Seq(new PointXY(w, n), new PointXY(e, n), new PointXY(e, s), new PointXY(w, s))
    val innerWall = WallPrefab(Some(gameCfg.tex(297)), Some(7), Some(8), Some(16), None, None)
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

  def axisAligned(a: PointXY, b: PointXY): Boolean = a.x == b.x || a.y == b.y

  /** for testing */
  def main(args: Array[String]): Unit = {
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
