package trn.render.polygon

import trn.BuildConstants.{DefaultCeilZ, DefaultFloorZ}
import trn.render.{Turtle, WallPrefab, MiscPrinter, TurtleOptions}
import trn.{PointXY, LineSegmentXY, BuildConstants, Map => DMap}

object PentagonPrinter {



  /**
    * Prints a pentagon.
    *
    * The pentagon will be formed by making all right turns from the initial segment.
    */
  def printPentagon(
    map: DMap,
    side: LineSegmentXY,
    gridSnap: Option[Int] = None,
  ): Int = {
    require(side.getManhattanLength > 1)

    // TODO: add snapping ability to turtle

    val t = Turtle(side, TurtleOptions(gridSnap))

    // it is 3, not 5, because we already have one side, and the last point is the first point
    for(_ <- 0 until 3) {
      t.turnRightD(72)
      t.forwardStamp(side.getLength)
    }

    MiscPrinter.createSector(map, t.wallLoop(), DefaultFloorZ, DefaultCeilZ)
  }

}
