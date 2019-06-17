package trn.prefab

import trn.PointXY

case class BoundingBox(xMin: Int, yMin: Int, xMax: Int, yMax: Int) {
  val w = xMax - xMin
  val h = yMax - yMin
  if(w < 0 || h < 0) throw new IllegalArgumentException

  def add(x: Int, y: Int): BoundingBox = {
    BoundingBox(
      math.min(xMin, x),
      math.min(yMin, y),
      math.max(xMax, x),
      math.max(yMax, y)
    )
  }

  def points: Set[PointXY] = Set(
    new PointXY(xMin, yMin),
    new PointXY(xMin, yMax),
    new PointXY(xMax, yMin),
    new PointXY(xMax, yMax),
  )

  def area: Int = w * h

  /**
    * Tests box size only, not location.
    * @param width
    * @param height
    * @return true if this bounding box could fix inside a rectangle of the given width and height
    */
  def fitsInside(width: Int, height: Int): Boolean = {
    if(width < 0 || height < 0) throw new IllegalArgumentException("height and width must be >= 0")
    w <= width && h <= height
  }

  /**
    * Tests whether this bounding box IS inside `b`.  Note that this tests both position and location.
    * @param b
    * @return
    */
  def isInsideInclusive(b: BoundingBox): Boolean = {
    return b.xMin <= xMin && xMax <= b.xMax && b.yMin <= yMin && yMax <= b.yMax
  }

  /**
    * @return true if the point is inside this BB, inclusive.
    */
  def contains(p: PointXY): Boolean = {
    xMin <= p.x && p.x <= xMax && yMin <= p.y && p.y <= yMax
  }

  def containsAny(points: PointXY*): Boolean = {
    points.foldLeft(false)(_ || contains(_))
  }

  def containsAny(points: Set[PointXY]): Boolean = containsAny(points.toSeq:_*)

  def intersect(b: BoundingBox): Option[BoundingBox] = {
    // special case intersection for boxes that are always at right angles to the axis
    val x1 = math.max(this.xMin, b.xMin)
    val x2 = math.min(this.xMax, b.xMax)
    val y1 = math.max(this.yMin, b.yMin)
    val y2 = math.min(this.yMax, b.yMax)
    if(x1 <= x2 && y1 <= y2){
      Some(BoundingBox(x1, y1, x2, y2))
    }else{
      None
    }
  }

  /**
    * Tests for overlap, but does not count overlap if only the edges overlap.
    * @param b
    * @return true if this bounding box overlaps with b, not counting overlapping edges.
    */
  // def overlapsExclusive(b: BoundingBox): Boolean = {
  //   if(this.w < 1 || this.h < 1 || b.w < 1 || b.h < 1){
  //     return true;
  //   }

  // }

  /**
    * @param dest coordinate to move bounding box to
    * @return bounding box with top left at the given coordinate
    */
  def translate(translation: PointXY): BoundingBox = {
    BoundingBox(
      xMin + translation.x,
      yMin + translation.y,
      xMax + translation.x,
      yMax + translation.y,
    )
  }

  def topLeft: PointXY = {
    new PointXY(xMin, yMin)
  }

  /**
    *  returns the vector that would move the top left of this bounding box to that point
    */
  def getTranslateTo(point: PointXY): PointXY = {
    val p1 = topLeft
    p1.translateTo(point)
  }

}
