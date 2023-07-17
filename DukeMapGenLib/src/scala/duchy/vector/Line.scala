package duchy.vector

import trn.{Sprite, PointXY, WallView}

/**
  *
  * @param a a point
  * @param b a vector from `a` (`b` is NOT a point)
  */
case class Line2D(a: PointXY, b: PointXY, lineType: Int) {
  def asLine: Line2D = this.copy(lineType = Line2D.LINE)
  def asRay: Line2D = this.copy(lineType = Line2D.RAY)
  def asSegment: Line2D = this.copy(lineType = Line2D.SEGMENT)

  def asType(lt: Int): Line2D = this.copy(lineType = lt)

}

object Line2D {
  val LINE = 0
  val RAY = 1
  val SEGMENT = 2


  def line(a: PointXY, b: PointXY): Line2D = Line2D(a, b, LINE)

  def lineFromPoints(a: PointXY, b: PointXY): Line2D = Line2D(a, b.subtractedBy(a), LINE)

  def ray(a: PointXY, b: PointXY): Line2D = Line2D(a, b, RAY)

  // def segment(a: PointXY, b: PointXY): Line2D = Line2D(a, b, LINE)

  // `b` - `a` is a vector from `a` to `b`
  def segmentFromPoints(pointA: PointXY, pointB: PointXY): Line2D = Line2D(pointA, pointB.subtractedBy(pointA), SEGMENT)

  def spriteRay(sprite: Sprite): Line2D = Line2D.ray(sprite.getLocation.asXY, sprite.getVector)

  def wallSegment(wallView: WallView): Line2D = Line2D.segmentFromPoints(wallView.p1, wallView.p2)
}
