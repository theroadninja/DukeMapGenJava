package trn.render.polygon

import trn.{PointXY, LineSegmentXY}

object PentagonPrinter {



  /**
    * Prints a pentagon.
    */
  def printPentagonUp(
    side: LineSegmentXY,
  ): Unit = {
    require(side.getManhattanLength > 1)
    // TODO: figure out how to snap to grid


  }

}
