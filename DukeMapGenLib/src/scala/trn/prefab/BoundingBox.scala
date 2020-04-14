package trn.prefab

import trn.PointXY

object BoundingBox {

  /**
    * @return a bounding box containing all the points
    */
  def apply(points: Seq[PointXY]): BoundingBox = {
    require(points.size > 0)
    points.foldLeft(BoundingBox(points(0))){ case (bb, p) => bb.add(p) }
  }

  def apply(p: PointXY): BoundingBox = BoundingBox(p.x, p.y, p.x, p.y)


  /**
    * merges the given bounding box into the set, discarding it if it fits inside any existing box,
    * or any boxes that fit inside the newcomer.
    *
    * To use this to merge a Seq of boxes:
    *   boxes.foldLeft(Seq.empty[BoundingBox])(BoundingBox.merge))
    *
    * @param boxes existing seq of boxes
    * @param b the new bounding box
    * @return seq of boxes merged with b
    */
  def merge(boxes: Seq[BoundingBox], b: BoundingBox): Seq[BoundingBox] = {
    val results = Seq.newBuilder[BoundingBox]
    var used = false
    boxes.foreach { existing =>
      if(existing.isInsideInclusive(b)){
        if(!used){
          results += b
          used = true
        }
      } else {
        results += existing
        if((!used) && b.isInsideInclusive(existing)){
          used = true
        }
      }
    }
    if(!used){
      results += b
    }
    results.result
  }

  /**
    *
    * @param group1
    * @param group2
    * @return true if any boundings box in group 1 has non-zero overlap with a box in group 2
    *         (if two boxes have edges touching, that wont count)
    */
  def nonZeroOverlap(group1: Traversable[BoundingBox], group2: Traversable[BoundingBox]): Boolean = {
    def area(b1: BoundingBox, b2: BoundingBox): Int = b1.intersect(b2).map(_.area).getOrElse(0)
    group1.map(b1 => group2.map(b2 => area(b1, b2)).sum).sum > 0
  }
}

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

  def add(p: PointXY): BoundingBox = add(p.x, p.y)

  def points: Set[PointXY] = Set(
    new PointXY(xMin, yMin),
    new PointXY(xMin, yMax),
    new PointXY(xMax, yMin),
    new PointXY(xMax, yMax),
  )

  def area: Int = w * h

  /**
    * @return if both bounding boxes have the same shape (without rotating)
    */
  def sameShape(other: BoundingBox): Boolean = w == other.w && h == other.h

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

  /** Tests whether this COULD fit inside bb (ignores location) */
  def fitsInsideBox(bb: BoundingBox): Boolean = fitsInside(bb.w, bb.h)

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

  def intersects(b: BoundingBox): Boolean = intersect(b).map(_.area).getOrElse(0) > 0

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

  def transform(matrix2: Matrix2D): BoundingBox = {
    val (xMin2, yMin2) = matrix2 * (xMin, yMin)
    val (xMax2, yMax2) = matrix2 * (xMax, yMax)
    require(xMin2 <= xMax2 && yMin2 <= yMax2) // we could automatically adjust these, but not sure if thats necessary
    BoundingBox(xMin2, yMin2, xMax2, yMax2)
  }

}
