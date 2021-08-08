package trn.bespoke.moonbase2

import trn.{BuildConstants, FVectorXY, HardcodedConfig, LineSegmentXY, LineXY, Main, MapLoader, PlayerStart, PointXY, PointXYZ, Sector, Wall, WallView, Map => DMap}
import trn.prefab.{BoundingBox, DukeConfig, GameConfig, MapWriter, RandomX}
import trn.render.{MiscPrinter, Texture, TextureUtil}

import scala.collection.JavaConverters._

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


object LoungePrinter {

  // TODO i want the option to print to the same Map, or to a new Sector Group (which has a self-contained map object)
  // pasting to a sector group and copying in is good for things like activator lotags

  /*

case class StairEntrance(p0: PointXY, p1: PointXY, floorZ: Int, ceilZ: Int, sectorId: Option[Int]) {
   */


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
    * @param point0A first point of connecting wall in room A
    * @param point1A second point of connecting wall in room A
    * @param entAfloorZ floor Z of room A
    * @param entAceilZ  ceiling Z of room A
    *
    * @param point0B first point of connecting wall in room B
    * @param point1B second point of connecting wall in room B
    * @param entBfloorZ floor Z of room B
    * @param entBceilZ ceiling Z of room B
    */
  def printLounge(
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
    require(axisAligned(point0A, point1A) && axisAligned(point0B, point1B))

    // first, we need to make little entrances, because the given walls could be flush with other walls in the sector
    // and it will look weird
    // TODO:  move walls (point0A->point1A) and (point0B->point1B) forward along their normals
    val entranceLength = 512 // TODO make smaller
    val entranceWall = gameCfg.tex(258)
    val entrancePrefab = SectorPrefab(183, 181)

    val normalA = point0A.vectorTo(point1A).toF.rotatedCCW.normalized.toPointXY
    val (newPoint0A, newPoint1A) = drawEntrance(map, point0A, point1A, normalA, entranceLength, entranceWall, entrancePrefab)

    val normalB = point0A.vectorTo(point1A).toF.rotatedCCW.normalized.toPointXY
    val (newPoint0B, newPoint1B) = drawEntrance(map, point0B, point1B, normalB, entranceLength, entranceWall, entrancePrefab)


    // TODO parallelogram test




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

  /**
    * Given:         .
    *     . room A   .
    *     .          .
    *    p0A ----->  p1A
    *         |
    *         \/(normalA)
    *
    * Make:
    *
    *    p1A <-----  p0A
    *    p2 ------>  p3
    *    /\          |
    *    |          \/
    *    p1 <------ p0
    *
    * @param p0A
    * @param p1A
    * @param normalA vector pointing OUT OF whatever sector p0->p1 is a part of
    * @param length  distance from p1 to p2 and from p0 to p3
    * @return a new (point0, point1) on the other side of the new room (is as if p0,p1 is a cursor that moved forward
    *       after "printing" the room.
    */
  private def drawEntrance(map: DMap, p0A: PointXY, p1A: PointXY, normalA: PointXY, length: Int, wallTex: Texture, sectorPrefab: SectorPrefab): (PointXY, PointXY) = {
    def isUnit(p: PointXY): Boolean = (p.x == 0 && Math.abs(p.y) == 1) || (Math.abs(p.x) == 1 && p.y == 0)
    require(isUnit(normalA), s"not a normal vector: ${normalA}")

    val p3 = p0A
    val p2 = p1A
    val p1 = p1A.add(normalA.multipliedBy(length))
    val p0 = p0A.add(normalA.multipliedBy(length))

    //val WallTex = Texture(1097, gameCfg.textureWidth(1097)) // or 791, 1097
    val loop = Seq(p0, p1, p2, p3).map(p => MiscPrinter.wall(p, wallTex))
    TextureUtil.setWallXScale(loop, 1.0)
    val sectorId = map.createSectorFromLoop(loop: _*)
    val sector = map.getSector(sectorId)
    sectorPrefab.writeTo(sector)
    (p0, p1)
  }

  def axisAligned(a: PointXY, b: PointXY): Boolean = a.x == b.x || a.y == b.y

  /** for testing */
  def main(args: Array[String]): Unit = {
    testGetSectorZ()


    // val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    // val writer = MapWriter(gameCfg)

    // test(gameCfg, writer.getMap)

    // writer.disarmAllSkyTextures()
    // // writer.setAnyPlayerStart(force = true)
    // writer.getMap.setPlayerStart(new PlayerStart(512, 512, 0, PlayerStart.NORTH))
    // writer.sgBuilder.clearMarkers()
    // writer.checkSectorCount()
    // Main.deployTest(writer.outMap, "output.map", HardcodedConfig.getEduke32Path("output.map"))
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

  def test(gameCfg: GameConfig, map: DMap): Unit = {

    def p(x: Int, y: Int): PointXY = new PointXY(x, y)

    val p0 = p(0, 0)
    val p1 = p(0, 2048)
    val n = p0.vectorTo(p1).toF.rotatedCCW.normalized.toPointXY
    println(n)
    // val WallTex = Texture(258, gameCfg.textureWidth(1097)) // or 791, 1097
    val WallTex = gameCfg.tex(258)

    drawEntrance(map, p0, p1, n, 512, WallTex, SectorPrefab(183, 181))

    // def eastMostWall(sectorId: Int): WallView = {
    //   val walls = map.getAllWallLoopsAsViews(sectorId).asScala.map(_.asScala).flatten
    //   walls.filter(_.isAlignedY).sortBy(w => w.getLineSegment.getP1.x).last
    // }
    // def westMostWall(sectorId: Int): WallView = {
    //   val walls = map.getAllWallLoopsAsViews(sectorId).asScala.map(_.asScala).flatten
    //   walls.filter(_.isAlignedY).sortBy(w => w.getLineSegment.getP1.x).head
    // }
    // val sector0 = MiscPrinter.box(map, p(start, 0), p(start + boxWidth, 2048), floor0, ceil0)
    // val wall0 = eastMostWall(sector0)
    // val e0 = StairEntrance(wall0, map.getSector(sector0), sector0)

    // val box1start = start + boxWidth + stairLength
    // val sector1 = MiscPrinter.box(map, p(box1start, 0), p(box1start + boxWidth, 2048), floor2, ceil2)
    // val wall1 = westMostWall(sector1)
    // val e1 = StairEntrance(wall1, map.getSector(sector1), sector1)
    // StairPrinter.straightStairs(map, e0, e1, WallTex)
  }


}
