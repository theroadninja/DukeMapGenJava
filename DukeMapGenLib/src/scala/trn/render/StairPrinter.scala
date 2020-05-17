package trn.render

import trn.math.Interpolate
import trn.{BuildConstants, FVectorXY, Main, MapLoader, PlayerStart, PointXY, PointXYZ, Wall, Map => DMap}
import trn.prefab.{GameConfig, PrefabPalette}

import scala.collection.JavaConverters._


object StairEntrance {
  def apply(p0: PointXY, p1: PointXY, floorZ: Int): StairEntrance = {
    StairEntrance(p0, p1, floorZ, floorZ - BuildConstants.DefaultSectorHeight)
  }

}
case class StairEntrance(p0: PointXY, p1: PointXY, floorZ: Int, ceilZ: Int) {
  def isDiagonal: Boolean = p0.x != p1.x && p0.y != p1.y
}

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

  def wall(p: PointXY, tex: Texture): Wall = {
    val wall = new Wall(p.x, p.y)
    wall.setXRepeat(8)
    wall.setYRepeat(8)
    wall.setTexture(tex.picnum)
    wall
  }


  def autoLinkRedWalls(map: DMap, sectorId0: Int, sectorId1: Int): Unit ={
    map.getAllWallLoopsAsViews(sectorId0).asScala.flatMap(_.asScala).foreach { w0 =>
      map.getAllWallLoopsAsViews(sectorId1).asScala.flatMap(_.asScala).foreach { w1 =>
        if(w0.getLineSegment == w1.getLineSegment.reversed()){
          map.linkRedWallsStrict(sectorId0, w0.getWallId, sectorId1, w1.getWallId)
        }else{
          println(s"${w0.getLineSegment()} vs ${w1.getLineSegment}")
        }
      }
    }
  }

  def straightStairs(
    map: DMap,
    e0: StairEntrance,
    e1: StairEntrance,
    wallTex: Texture
  ): Unit = {
    require(!e0.isDiagonal)
    require(!e1.isDiagonal)

    val StairCount = 8
    //val StepSize = 1024

    val sideL = Interpolate.linear(e0.p0, e1.p1, StairCount)
    val sideR = Interpolate.linear(e0.p1, e1.p0, StairCount)
    val floorZs = Interpolate.linear(e0.floorZ, e1.floorZ, StairCount)

    // loops are always made clockwise
    var prevSector = -1
    for(i <- 0 until StairCount - 1){
      val j = i + 1
      val loop = Seq(sideL(i), sideL(j), sideR(j), sideR(i)).map(p => wall(p, wallTex))
      TextureUtil.setWallXScale(loop, 1.0)
      val sId = map.createSectorFromLoop(loop: _*)
      map.getSector(sId).setFloorZ(floorZs(i))
      map.getSector(sId).setCeilingZ(floorZs(i) - BuildConstants.DefaultSectorHeight * 2)
      if(prevSector != -1){
        autoLinkRedWalls(map, sId, prevSector)
      }
      prevSector = sId


    }


    // single box
    // val loop = Seq(e0.p1, e0.p0, e1.p1, e1.p0).map(p => wall(p, wallTex))
    // TextureUtil.setWallXScale(loop, 1.0)
    // map.createSectorFromLoop(loop: _*)

  }
}
