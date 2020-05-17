package trn.render

import trn.math.Interpolate
import trn.{BuildConstants, FVectorXY, LineSegmentXY, Main, MapLoader, PlayerStart, PointXY, PointXYZ, Sector, Wall, Map => DMap}
import trn.prefab.{GameConfig, PrefabPalette}

import scala.collection.JavaConverters._


object StairEntrance {
  def apply(p0: PointXY, p1: PointXY, sector: Sector): StairEntrance = {
    // TODO - use ceilingZ
    StairEntrance(p0, p1, sector.getFloorZ, sector.getFloorZ - BuildConstants.DefaultSectorHeight, None)
  }

}
case class StairEntrance(p0: PointXY, p1: PointXY, floorZ: Int, ceilZ: Int, sectorId: Option[Int]) {
  def isDiagonal: Boolean = p0.x != p1.x && p0.y != p1.y
}

case class StairTextures(wallTex: Texture)

// TODO - support auomatic light intersection (like in curved stairwell: one side of inner colum is a light...)
// TODO - when interpolating; add an extra check to make sure the integer vertexes are actually different
// Notes on approximating a half circle with a bezier curve
// http://digerati-illuminatus.blogspot.com/2008/05/approximating-semicircle-with-cubic.html
object StairPrinter {

  /**
    * So, I was hoping that straight stairs could be a special case of curved stairs, but with bezier curvers that
    * just doesnt work (stairs are uneven sizes).  Note: this doesnt have the stairs yet; its just a curved hallway.
    *
    * It works by tracing a "guide" from the midpoint of one entrance to the other, and then placing the walls
    * on either side of the guide.
    *
    * The entrances must be described by points A,B (e.g. leftP0, leftP1) where the direction of the stair is the
    * vector A->B rotated CCW (to the left).
    *
    * @param map  map to draw to
    * @param leftP0  first coord of entrance 1
    * @param leftP1  second coord of entrance 1
    * @param rightP0  first coord of entrance 2
    * @param rightP1  second coord of entrace 2
    * @param tex  texture will be applied to all walls
    */
  def writeCurvedStairs(
    map: DMap,
    leftP0: PointXY,
    leftP1: PointXY,
    rightP0: PointXY,
    rightP1: PointXY,
    tex: Texture,
    pointCount: Int = 16
  ): Unit = {
    val w0 = leftP0.distanceTo(leftP1).toInt
    val w1 = rightP0.distanceTo(rightP1).toInt

    val guide0 = PointXY.midpoint(leftP0, leftP1)
    val guide1 = PointXY.midpoint(rightP0, rightP1)

    val handleLength = guide0.distanceTo(guide1).toInt * 2/3  // 2/3 is best for approximating circles?

    val guide0h_vector = leftP0.vectorTo(leftP1).toF.rotatedCCW.normalized.scaled(handleLength).toPointXY
    val guide0h = guide0.add(guide0h_vector)

    val guide1h_vector = rightP0.vectorTo(rightP1).toF.rotatedCCW.normalized.scaled(handleLength).toPointXY
    val guide1h = guide1.add(guide1h_vector)

    val guideLoop = Interpolate.cubicWithTangents(guide0, guide0h, guide1h, guide1, pointCount)
    val widths = Interpolate.linear(w0, w1, pointCount)
    val loops = (1 until guideLoop.size - 1).map { pointIndex =>
      val p = guideLoop(pointIndex)
      val width = widths(pointIndex)
      val (tanx, tany) = p.tangent
      val leftVec = new FVectorXY(tanx, tany).rotatedCCW.scaled(width/2).toPointXY
      val rightVec = new FVectorXY(tanx, tany).rotatedCW.scaled(width/2).toPointXY
      val rightPoint = p.p.add(rightVec)
      val leftPoint = p.p.add(leftVec)
      (leftPoint, rightPoint)
    }

    val (leftLoop, rightLoop) = (Seq((leftP0, leftP1)) ++ loops ++ Seq((rightP1, rightP0))).unzip

    def w(p: PointXY): Wall = {
      val wall = new Wall(p.x, p.y)
      // wall.setXRepeat(8)
      wall.setYRepeat(8)
      wall.setTexture(tex.picnum)
      wall
    }

    val loop = leftLoop ++  rightLoop.reverse // works
    val loop2: Seq[Wall] = loop.map(w)

    TextureUtil.lineUpTextures(loop2, 1.0, tex.widthPx)

    map.createSectorFromLoop(loop2: _*)
  }

  // TODO - dry with MiscPrint ?
  def wall(p: PointXY, tex: Texture): Wall = {
    val wall = new Wall(p.x, p.y)
    wall.setXRepeat(8)
    wall.setYRepeat(8)
    wall.setTexture(tex.picnum)
    wall
  }


  def straightStairs(
    map: DMap,
    e0: StairEntrance,
    e1: StairEntrance,
    wallTex: Texture,
    stairCount: Option[Int] = None,
    linkSectors: Option[Seq[Int]] = None
  ): Unit = {
    require(!e0.isDiagonal)
    require(!e1.isDiagonal)

    // COUNTING FOR DUMMIES
    // observse that the highest "stair" is really just the next sector, so:
    // 4 stairs(StairCount) == 3 stair sectors == 5 stair heights
    // # walls = StairCount
    // # floors = StairCount + 1
    // # stair sectors = StairCount - 1
    // # divisions of height = StairCount (divide vertical drop by this to gets number of steps)
    val MaxStairHeight = BuildConstants.MaxStepsWalk * BuildConstants.ZStepHeight
    val stairCount2 = stairCount.getOrElse{
      val verticalDrop = Math.abs(e0.floorZ - e1.floorZ)
      // TODO - do better than this:  we can very both stair height and stair width
      verticalDrop /  MaxStairHeight
    }
    //val StairCount = 4

    // the lowest stair is the second lowest level; the lowest level is in the sector before the lower stair
    val sideL = Interpolate.linear(e0.p0, e1.p1, stairCount2)
    val sideR = Interpolate.linear(e0.p1, e1.p0, stairCount2)
    val floorZs = Interpolate.linear(e0.floorZ, e1.floorZ, stairCount2 + 1)

    // loops are always made clockwise
    var prevSector = -1
    for(i <- 0 until stairCount2 - 1){
      val j = i + 1
      val loop = Seq(sideL(i), sideL(j), sideR(j), sideR(i)).map(p => wall(p, wallTex))
      TextureUtil.setWallXScale(loop, 1.0)
      val sId = map.createSectorFromLoop(loop: _*)
      map.getSector(sId).setFloorZ(floorZs(j))
      map.getSector(sId).setCeilingZ(floorZs(j) - BuildConstants.DefaultSectorHeight * 2)
      //linkSectors.map(sectors => sectors.foreach(sId2 => autoLinkRedWalls(map, sId, sId2))) // link everything with stard/end, bc i'm lazy
      if(prevSector != -1){
        MiscPrinter.autoLinkRedWalls(map, sId, prevSector)
      }
      prevSector = sId
    }

    // single box
    // val loop = Seq(e0.p1, e0.p0, e1.p1, e1.p0).map(p => wall(p, wallTex))
    // TextureUtil.setWallXScale(loop, 1.0)
    // map.createSectorFromLoop(loop: _*)

  }

  def straightStairs(
    map: DMap,
    sectorId0: Int,
    wallId0: Int,
    sectorId1: Int,
    wallId1: Int,
    stairTex: StairTextures
  ): Unit = {


    def toEntrance(sector: Sector, wall: LineSegmentXY): StairEntrance = {
      StairEntrance(
        wall.getP2,
        wall.getP1,
        sector.getFloorZ,
        sector.getCeilingZ,
        None
      )
    }

    val e0 = toEntrance(map.getSector(sectorId0), map.getWallView(wallId0).getLineSegment)
    val e1 = toEntrance(map.getSector(sectorId1), map.getWallView(wallId1).getLineSegment)
    straightStairs(map, e0, e1, stairTex.wallTex, None, Some(Seq(sectorId0, sectorId1)))
  }

}
