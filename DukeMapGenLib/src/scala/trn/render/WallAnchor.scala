package trn.render

import trn.{BuildConstants, LineSegmentXY, LineXY, PointXY, Sector, WallView}

/**
  * Used to mark the location of a SINGLE wall where printing should start or end, or include.
  * Should use a different object if multiple walls are needed.
  *
  * NOTE: I dont want to rely on "sector" or "wall" objects at this level (though providing convenience methods is fine)
  *
  * See Also:  RedwallConnector
  * TODO: see also StairEntrance which was an early form of this
  */
case class WallAnchor(
  p0: PointXY, p1: PointXY, floorZ: Int, ceilZ: Int
) {
  def width: Double = p0.distanceTo(p1)

  val axisAligned: Boolean = p0.x == p1.x || p0.y == p1.y

  /** @returns the vector p0->p1 */
  def vector: PointXY = p1.subtractedBy(p0)

  def points: Seq[PointXY] = Seq(p0, p1)

  def reversed: WallAnchor = WallAnchor(p1, p0, floorZ, ceilZ)
}

object WallAnchor {

  /**
    * Create the wall anchor from a wall that already exists in a sector, making 2 assumptions:
    * 1) whatever is using the wall anchor is going to create a new sector to join to the existing one that this
    *   wall comes from
    * 2) the code that reads this wall anchor wants the points in the order that it would draw them.
    *
    * Visual explanation:
    *
    *  ------------------> p1           p1
    *                      |            /\
    *    Existing Sector   |  becomes:  |  Wall Anchor
    *                      |            |
    *                     \/            |
    *  <----------------- p2            p0
    *
    *  so the points get reversed (although I started one with 1 and one with 0 anyway...)
    *
    *
    * @param wall
    * @param floorZ
    * @param ceilZ
    * @return
    */
  def fromExistingWall(
    wall: WallView,
    floorZ: Int = BuildConstants.DefaultFloorZ,
    ceilZ: Int = BuildConstants.DefaultCeilZ
  ): WallAnchor = {
    WallAnchor(wall.getLineSegment.getP2, wall.getLineSegment.getP1, floorZ, ceilZ)
  }

  def fromExistingWall2(
    wall: WallView,
    sector: Sector
  ): WallAnchor = {
    WallAnchor(wall.getLineSegment.getP2, wall.getLineSegment.getP1, sector.getFloorZ, sector.getCeilingZ)

  }
}
