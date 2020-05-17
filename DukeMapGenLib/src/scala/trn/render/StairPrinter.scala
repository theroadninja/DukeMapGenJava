package trn.render

import trn.math.Interpolate
import trn.{BuildConstants, Main, MapLoader, PlayerStart, PointXY, PointXYZ, Wall, Map => DMap}
import trn.prefab.{GameConfig, PrefabPalette}

import scala.collection.JavaConverters._


object Bezier {

  /**
    * A quadratic bezier curve involving line segments p0-p1 and p2-p3.
    */
  def quad(p0: PointXY, p1: PointXY, p2: PointXY, p3: PointXY): Seq[PointXY] = {


    ???
  }
}

object StairPrinter {

  // TODO - constants for number of steps duke can walk without jumping, and max number he can jump
  val ZStepHeight = BuildConstants.ZStepHeight

  // reads a test map and prints stuff, for reverse engineering xrepeat and xpan
  def xrepeatTest(loader: MapLoader, gameCfg: GameConfig): Unit = {
    val sourceMap = loader.load("xrepeat.map")
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap, true);

    // For measuring x-repeat:
    val allWalls = palette.allSectorGroups().asScala.flatMap(sg => sg.getAllWallViews).toSeq
    // allWalls.filter(_.tex() != 0).foreach(w => println(s"${w.tex}, xrepeat=${w.xRepeat} length=${w.length}"))
    allWalls.filter(_.tex != 0).foreach { w =>
      println(s"tex=${w.tex} xrepeat=${w.xRepeat} xpan=${w.xPan()}")
    }
  }

  // TODO - DRY with FVectorXY
  def rotateCWAndScale(x: Double, y: Double, scale: Int): PointXY = {
    // Normally:
    // clockwize = (y, -x)
    // counter = (-y, x)
    // however:  because the Build editor shows Y+ going "down", this flips everything.
    // Build CW = (-y, x)
    // Build CCW = (y, -x)
    PointXY.fromDouble(-y * scale, x * scale)
  }
  def rotateCCWAndScale(x: Double, y: Double, scale: Int): PointXY = {
    // Normally:
    // clockwize = (y, -x)
    // counter = (-y, x)
    // however:  because the Build editor shows Y+ going "down", this flips everything.
    // Build CW = (-y, x)
    // Build CCW = (y, -x)
    PointXY.fromDouble(y * scale, -x * scale)
  }


  /**
    * So, I was hoping that straight stairs could be a special case of curved stairs, but with bezier curvers that
    * just doesnt work (stairs are uneven sizes)
    *
    * @param map
    * @param leftP0
    * @param leftP1
    * @param rightP0
    * @param rightP1
    * @param tex
    * @param gameCfg
    */
  def writeCurvedStairs(
    map: DMap,
    leftP0: PointXY,
    leftP1: PointXY,
    rightP0: PointXY,
    rightP1: PointXY,
    tex: Int,
    gameCfg: GameConfig
  ): Unit = {
    val w0 = 2048 // width
    val w1 = 2048

    val guide0 = PointXY.midpoint(leftP0, leftP1)
    val guide1 = PointXY.midpoint(rightP0, rightP1)

    // TODO - this should probably be distance, not manhattan distance
    val handleLength = guide0.distanceTo(guide1).toInt * 2/3  // 2/3 is best for approximating circles?
    // val handleLength = guide0.distanceTo(guide1).toInt * 1/10
    // val handleLength = guide0.distanceTo(guide1).toInt * 1/2
    //val handleLength = guide0.distanceTo(guide1).toInt * 2/5

    val guide0h_vector = leftP0.vectorTo(leftP1).toF.rotatedCCW.normalized.scaled(handleLength).toPointXY
    val guide0h = guide0.add(guide0h_vector)
    //val guide0h = guide0.add(new PointXY(0, -handleLength.toInt))

    val guide1h_vector = rightP0.vectorTo(rightP1).toF.rotatedCCW.normalized.scaled(handleLength).toPointXY
    val guide1h = guide1.add(guide1h_vector)
    // val guide1h = guide1.add(new PointXY(0, -handleLength.toInt))

    val guideLoop = Interpolate.cubicWithTangents(guide0, guide0h, guide1h, guide1, 16)
    val loops = (1 until guideLoop.size - 1).map { pointId =>
      val p = guideLoop(pointId)

      // TODO - interpolate width
      val width = w0

      val (tanx, tany) = p.tangent
      val leftVec = rotateCCWAndScale(tanx, tany, width/2)
      val rightVec = rotateCWAndScale(tanx, tany, width/2)

      val rightPoint = p.p.add(rightVec)
      val leftPoint = p.p.add(leftVec)
      (leftPoint, rightPoint)
    }

    val (leftLoop, rightLoop) = (Seq((leftP0, leftP1)) ++ loops ++ Seq((rightP1, rightP0))).unzip

    def w(p: PointXY): Wall = {
      val wall = new Wall(p.x, p.y)
      wall.setXRepeat(8)
      wall.setYRepeat(8)
      wall.setTexture(tex)
      wall
    }

    val loop = leftLoop ++  rightLoop.reverse // works
    val loop2: Seq[Wall] = loop.map(w)

    TextureUtil.lineUpTextures(loop2, 1.0, gameCfg.textureWidth(tex))



    map.createSectorFromLoop(loop2: _*)


  }

  def quickTest(loader: MapLoader, gameCfg: GameConfig): Unit = {
    val sourceMap = loader.load("stairs.map")
    // val sourceMap = loader.load("xrepeat.map")
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap, true);


    def p(x: Int, y: Int): PointXY = new PointXY(x, y)

    // Notes on approximating a half circle with a bezier curve
    // http://digerati-illuminatus.blogspot.com/2008/05/approximating-semicircle-with-cubic.html

    // TODO - support auomatic light intersection (like in curved stairwell: one side of inner colum is a light...)
    // TODO - when interpolating; add an extra check to make sure the integer vertexes are actually different
    val map: DMap = DMap.createNew





    // nice half-circle
    // val leftP0 = p(0, 0)
    // val leftP1 = p(2048, 0)
    // val rightP0 = p(4096 + 1024, 0)
    // val rightP1 = p(6144 + 1024, 0)

    val leftP0 = p(0, 0)
    val leftP1 = p(0, 2048)

    val rightP0 = p(4096, 2048)
    val rightP1 = p(4096, 0)

    val WallTex = 1097 // or 791, 1097
    writeCurvedStairs(map, leftP0, leftP1, rightP0, rightP1, 791, gameCfg)


    map.setPlayerStart(new PlayerStart(512, 512, 0, PlayerStart.NORTH))
    Main.deployTest(map)
  }
}
class StairPrinter {

}
