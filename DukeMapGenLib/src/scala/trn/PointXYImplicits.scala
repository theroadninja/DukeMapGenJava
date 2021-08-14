package trn

import trn.prefab.{IntMatrix, Matrix2D}

/** TODO PointImplicits is newer and better ... see if most of these functions can be deleted */
object PointXYImplicits {
  class PointXYExtended(point: PointXY) {

    // TODO - is this even correct?
    //def *(m: Matrix2D): PointXY = Matrix2D.multiply(point, m)

    def toSeq(): Seq[Int] = Seq(point.x, point.y)

    /**
      * turns this into a matrix with column [x, y, 1] for use in 2d transformations
      * @return
      */
    def toMatrix(): IntMatrix = {
      IntMatrix.toColumn(this.toSeq :+ 1)
    }

    def +(t: (Int, Int)): PointXY = new PointXY(point.x + t._1, point.y + t._2)

    def toTuple: (Int, Int) = (point.x, point.y)
  }

  implicit def pointXYExtended(point: PointXY) = new PointXYExtended(point)
}
