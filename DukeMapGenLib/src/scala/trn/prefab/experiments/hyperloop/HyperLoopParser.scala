package trn.prefab.experiments.hyperloop

import trn.WallView
import trn.prefab.SectorGroup

/**
  * Code for interpreting hyperloop source maps.
  */
object HyperLoopParser {

  // /**
  //   * Given a SectorGroup that is meant to represent the size of the space inside the inner ring, return the
  //   * radius from the center of the group, to the eastern wall.
  //   *
  //   * @param coreSg
  //   * @param innerE
  //   * @return
  //   */
  // def measureCoreRadius(coreSg: SectorGroup): Int = {
  //   val eastAndWest: Seq[WallView] = coreSg.getAllWallViews.filter(wall => wall.isAlignedY).toSeq
  //   require(eastAndWest.size == 2, "there should be exactly 2 walls aligned to the Y axis")

  //   // compare the x coord of the center of the bounding box with the X coords calculated from the two walls
  //   val centerX = coreSg.boundingBox.center.x
  //   val xValues = eastAndWest.map(_.getLineSegment.getP1.x)
  //   val center2 = (xValues(0) + xValues(1)) / 2
  //   require(centerX == center2)
  //   Math.abs(xValues(0) - centerX)
  // }

  /**
    * Given a sector group that has exactly 2 y-axis-aligned walls, return the distance between them
    * @param sg
    * @return
    */
  def measureWidth(sg: SectorGroup): Int = {
    val xValues = sg.getAllWallViews.filter(wall => wall.isAlignedY).map(_.getLineSegment.getP1.x).toSeq
    require(xValues.size == 2, "there should be exactly 2 walls aligned to the Y axis")
    Math.abs(xValues(0) - xValues(1))
  }


  /**
    * measure the distance from the "inner" connector to the anchor.  Only works with axis-aligned ring groups
    *
    * With this algorithm, only the "middle" ring sector groups can have anchors.
    *
    * @param sg - should be an "east" axis-aligned ring group
    * @return
    */
  def measureDistToAnchor(sg: SectorGroup): Int = {
    val conn = sg.getRedwallConnector(EdgeIds.InnerEdgeConn)
    require(conn.isAxisAligned, "connector must be axis-aligned")
    sg.getRedwallConnector(EdgeIds.InnerEdgeConn).getBoundingBox.center.manhattanDistanceTo(sg.getAnchor.asXY).toInt
  }


}
