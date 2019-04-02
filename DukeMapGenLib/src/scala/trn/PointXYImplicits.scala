package trn

import trn.prefab.{IntMatrix, Matrix2D}

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
  }

  implicit def pointXYExtended(point: PointXY) = new PointXYExtended(point)
}
