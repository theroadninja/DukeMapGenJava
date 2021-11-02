package trn

// TODO if this works out, move it somewhere else
object PointImplicits {
  // to use: import Helpers._
  implicit class PointXYScala(p: PointXY) {
    def + (other: PointXY): PointXY = p.add(other)

    def + (other: FVectorXY): PointXY = p.add(toI(other))

    def - (other: PointXY): PointXY = p.subtractedBy(other)

    /** integer division */
    def / (other: Int): PointXY = new PointXY(p.x / other, p.y / other)
  }

  implicit class FVectorXYScala(p: FVectorXY) {
    def * (other: Double): FVectorXY = p.multipliedBy(other)

    def toI: PointXY = new PointXY(p.x.toInt, p.y.toInt)
  }

  def toI(f: FVectorXY): PointXY = new PointXY(f.x.toInt, f.y.toInt)

  def axisAligned(a: PointXY, b: PointXY): Boolean = a.x == b.x || a.y == b.y
}
