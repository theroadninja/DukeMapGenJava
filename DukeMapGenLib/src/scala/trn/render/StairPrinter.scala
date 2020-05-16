package trn.render

import trn.math.Interpolate
import trn.{BuildConstants, Main, MapLoader, PlayerStart, PointXY, PointXYZ, Wall, Map => DMap}
import trn.prefab.PrefabPalette

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

  // TODO - when interpolating; add an extra check to make sure the integer vertexes are actually different

  def quickTest(loader: MapLoader): Unit = {
    val sourceMap = loader.load("stairs.map")
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap, true);

    palette.allSectorGroups().asScala.flatMap(sg => sg.allWalls).foreach(w => println(w.getXRepeat()))

    def w(p: PointXY): Wall = {
      val wall = new Wall(p.x, p.y)
      wall.setXRepeat(8)
      wall.setYRepeat(8)
      wall
    }
    def p(x: Int, y: Int): PointXY = new PointXY(x, y)

    // def midpoint(p0: PointXY, p1: PointXY): PointXY = {
    // }


    // Notes on approximating a half circle with a bezier curve
    // http://digerati-illuminatus.blogspot.com/2008/05/approximating-semicircle-with-cubic.html

    val map: DMap = DMap.createNew

    // // This code makes the north wall a curved dome
    // val nw = new PointXY(0, 0)
    // val ne = new PointXY(2048, 0)
    // val sw = new PointXY(0, 1024)
    // val se = new PointXY(2048, 1024)
    // // i want the curve to be a half circle if my "control arms" are facing straight out
    // // multiplying the "arm" length by 2/3 seems to do the trick
    // val handleWidth = 2048 * 2 / 3
    // val handle = new PointXY(0, -handleWidth)
    // val topRow = Interpolate.cubic(nw, nw.add(handle), ne.add(handle), ne, 8)
    // val loop = topRow.map(p => w(p)) ++ Seq(w(se), w(sw))
    // val sectorId = map.createSectorFromLoop(loop: _*)

    val leftP0 = p(0, 0)
    val leftP1 = p(2048, 0)
    val rightP0 = p(4096, 0)
    val rightP1 = p(6144, 0)

    val w0 = 2048 // width
    val w1 = 2048

    val guide0 = p(1024, 0)
    val guide0vector = p(0, 1)
    val guide1 = p(5120, 0)
    val guide1vector = p(0, 1)

    val handleLength = guide0.manhattanDistanceTo(guide1) * 2 / 3
    //val guide0h = guide0vector.multipliedBy(handleLength.toInt)
    //val guide1h = guide1vector.multipliedBy(handleLength.toInt)
    val guide0h = guide0.add(new PointXY(0, -handleLength.toInt))
    val guide1h = guide1.add(new PointXY(0, -handleLength.toInt))

    val guideLoop = Interpolate.cubicWithTangents(guide0, guide0h, guide1h, guide1, 16)
    val loops = (1 until guideLoop.size - 1).map { pointId =>
      val p = guideLoop(pointId)

      // val prev = guideLoop(pointId-1).p
      // val next = guideLoop(pointId+1).p

      // TODO - interpolate width
      val width = w0
      val w = width/2

      val (tanx, tany) = p.tangent

      // Normally:
      // clockwize = (y, -x)
      // counter = (-y, x)
      // however:  because the Build editor shows Y+ going "down", this flips everything.
      // Build CW = (-y, x)
      // Build CCW = (y, -x)
      val leftVec = PointXY.fromDouble(tany * w, -tanx * w) // CW
      val rightVec = PointXY.fromDouble(-tany * w, tanx * w) // CCW

      val rightPoint = p.p.add(rightVec)
      val leftPoint = p.p.add(leftVec)
      (leftPoint, rightPoint)
    }

    // val (leftLoop, rightLoop) = (Seq((leftP0, leftP1)) ++ loops ++ Seq((rightP1, rightP0))).unzip
    val (leftLoop, rightLoop) = (Seq((leftP0, leftP1)) ++ loops ++ Seq((rightP1, rightP0))).unzip


    // TODO - i think walls need to be specified CCW -- wait, no, CW, as you are looking at the build screen?

    val loop = leftLoop ++  rightLoop.reverse // works
    // val loop = Seq(leftP0, leftP1) ++ /*rightLoop ++*/ leftLoop.reverse
    // val loop = Seq(leftP0) ++ leftLoop ++  Seq(rightP1, rightP0) ++ rightLoop.reverse
    val loop2: Seq[Wall] = loop.map(w)
    map.createSectorFromLoop(loop2: _*)


    map.setPlayerStart(new PlayerStart(512, 512, 0, PlayerStart.NORTH))
    Main.deployTest(map)
  }
}
class StairPrinter {

}
