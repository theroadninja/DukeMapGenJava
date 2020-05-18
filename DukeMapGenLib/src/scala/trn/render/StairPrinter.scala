package trn.render

import trn.math.Interpolate
import trn.{BuildConstants, FVectorXY, LineSegmentXY, Main, MapLoader, PlayerStart, PointXY, PointXYZ, Sector, Wall, WallView, Map => DMap}
import trn.prefab.{GameConfig, PrefabPalette}

import scala.collection.JavaConverters._


object StairEntrance {
  def apply(w: WallView, sector: Sector, sectorId: Int): StairEntrance = {
    StairEntrance(w.getLineSegment.getP1, w.getLineSegment.getP2, sector, sectorId)
  }
  def apply(p0: PointXY, p1: PointXY, sector: Sector, sectorId: Int): StairEntrance = {
    // TODO - use ceilingZ
    // StairEntrance(p0, p1, sector.getFloorZ, sector.getFloorZ - BuildConstants.DefaultSectorHeight, Some(sectorId))
    StairEntrance(p0, p1, sector.getFloorZ, sector.getCeilingZ, Some(sectorId))
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

  def snapToNearest(value: Int, increment: Int): Int = value / increment * increment

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

  /**
    * Calculates the vertical drop (in units of pgUp/pgDown steps) of each stair based on the given slope.
    *
    * This is a subjective value that I established by eyeballing it.
    *
    * @param slope  the ratio of ( vertical drop / stair length) -- note that you must convert the z units to xy units
    *                but shifting it 4 to the right.
    * @return one of 0, 1, 2, 3, 4 for the step size
    */
  def stepSizeForSlope(slope: Double): Int = {
    require(slope >= 0)
    // val MaxSlopeFor2 = 0.719101  // NOTE:  they sure dont look good though
    val MaxSlopeFor3 = 0.727272727272 // about 36.027
    // this is a good place to switch between 3 and 2:
    // slope 0.5161290322580645 angle: 27.299572211332805
    if(slope > MaxSlopeFor3){
      4
    }else if(slope > 0.5161290322580645){ // not max slope for 3
      3
    }else if(slope > 0.125) {
      2
    }else if(slope > 0.008){  // 0.5 degree
      1
    }else{
      0
    }
    // notes:
    // switching betwee 2 and 3?
    // slope 0.5161290322580645 angle: 27.299572211332805
    // good place to switch to 1:
    //  slope: 0.125 angle: 7.125016348901798
  }


  /**
    * Drawns stairs on the map.
    *
    * Entrance notes:
    *  The entrances have to be specified such that for a wall (A, B), if you take the vector A->B and rotate it left (CCW)
    *  then you get the direction the stairs are going.  Unfortunately this is actually backwards from how the build
    *  engine works...maybe this should be automatic?
    *
    * Script mode / max slope notes:
    *  stairs can be arbitrarily steep, because the sectors can be razor thin.
    *  so to define a "minimum" length for stairs, or really the maximum slope,
    *  i looked at how step they could be before Duke starts jumping down them
    *  at walking speed.  Roughly 40 degrees seems to be the max you can get without
    *  any jumping/falling.  So, with script mode on, this is enforced.  If you dont
    *  like it, turn script mode off.
    *
    *  NOTE:  according to the internet, 37 degrees is the preferred stair angle
    *
    * If the vertical drop is exactly zero, it will correct draw a hallway
    *
    * @param map
    * @param e0
    * @param e1
    * @param wallTex
    * @param strict
    * @return
    */
  def straightStairs(
    map: DMap,
    e0: StairEntrance,
    e1: StairEntrance,
    wallTex: Texture,
    strict: Boolean = true
  ): Seq[Int] = {
    require(!e0.isDiagonal)
    require(!e1.isDiagonal)

    val verticalDrop = Math.abs(e0.floorZ - e1.floorZ)
    val stairLength = Math.max(e0.p0.distanceTo(e1.p1), e0.p1.distanceTo(e1.p0))
    val slope = BuildConstants.ztoxy(verticalDrop).toDouble / stairLength
    if(strict){
      // decent calculator: https://www.mathsisfun.com/scientific-calculator.html

      // NOTE:  this is the max slope for stairs of step size 4.  The max is reduced for smaller stairs
      // see stepSizeForSlope()
      val MaxSlope = 0.84506 // 40.2 degrees, angle of ascent.  (I actually tested slope .84210 though)

      if(slope > MaxSlope){
        throw new IllegalArgumentException(s"Strict mode: slope ${slope} exceeds max ${MaxSlope}")
      }
    }
    val stairHeight = stepSizeForSlope(slope) * BuildConstants.ZStepHeight


    // min count is 2 for linear interpolation
    val stairCount2 = Math.max(2, if(stairHeight == 0){ 0 }else{ verticalDrop / stairHeight })

    // COUNTING FOR DUMMIES
    // observse that the highest "stair" is really just the next sector, so:
    // # divisions of height = StairCount (divide vertical drop by this to gets number of steps)
    // # step wall = StairCount
    // # floors (including start and end) = StairCount + 1
    // # stair sectors = StairCount - 1
    // 4 stairs(StairCount) == 3 stair sectors == 5 stair heights
    //
    //                    .      .      .      .  (not stair sector)
    //                    .      .      .      4--------------------
    //                    .      .       stair2|
    //                    .      .      3------+
    //                    .       stair1|
    //                    .      2------+
    //                     stair0|
    //                    1------+
    // (not stair sector) |
    // -------------------+
    // the lowest stair is the second lowest level; the lowest level is in the sector before the lower stair
    // the highest "stair" and highest level is not a stair sector, but the ending sector
    val sideL = Interpolate.linear(e0.p0, e1.p1, stairCount2)
    val sideR = Interpolate.linear(e0.p1, e1.p0, stairCount2)
    val floorZs = Interpolate.linear(e0.floorZ, e1.floorZ, stairCount2 + 1)
    val ceilZs = Interpolate.linear(e0.ceilZ, e1.ceilZ, stairCount2 + 1)

    // loops are always made clockwise
    var prevSector = -1
    val sectorIds = (0 until stairCount2 - 1).map { i =>
      val j = i + 1
      val loop = Seq(sideL(i), sideL(j), sideR(j), sideR(i)).map(p => wall(p, wallTex))
      TextureUtil.setWallXScale(loop, 1.0)
      val sId = map.createSectorFromLoop(loop: _*)

      // snap to nearest 1024 so we dont do something people using the build editor cant do
      map.getSector(sId).setFloorZ(snapToNearest(floorZs(j), BuildConstants.ZStepHeight))
      map.getSector(sId).setCeilingZ(snapToNearest(ceilZs(j), BuildConstants.ZStepHeight))
      if(prevSector != -1){
        MiscPrinter.autoLinkRedWalls(map, sId, prevSector)
      }
      prevSector = sId
      sId
    }

    val linkSectors  = Seq(e0.sectorId, e1.sectorId).flatten
    linkSectors.foreach{sId2 =>
      MiscPrinter.autoLinkRedWalls(map, sectorIds.head, sId2)
      MiscPrinter.autoLinkRedWalls(map, sectorIds.last, sId2)
    }
    sectorIds
  }


  // for the straight stairs, though maybe curved will also work
  def paintStairs(): Unit = {
    // TODO - dont forget the wall on the inside of the lower sector!  might need to make it a '2' wall
    ???
  }

}
