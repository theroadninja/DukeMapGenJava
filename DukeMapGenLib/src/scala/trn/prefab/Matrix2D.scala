package trn.prefab

import trn.PointXY
import trn.PointXYImplicits._

/**
  * https://www.mathplanet.com/education/geometry/transformations/transformation-using-matrices
  *
  * better explanation:
  * http://www.it.hiof.no/~borres/j3d/math/twod/p-twod.html
  */
object Matrix2D {
  val SIZE = 3

  def translate(dx: Int, dy: Int): Matrix2D = {
    Matrix2D(
      Seq(1, 0, dx),
      Seq(0, 1, dy),
      Seq(0, 0, 1)
    )
  }

  def flipX: Matrix2D = {
    Matrix2D(
      Seq(-1, 0, 0),
      Seq(0, 1, 0),
      Seq(0, 0, 1)
    )
  }

  def flipY: Matrix2D = {
    Matrix2D(
      Seq(1, 0, 0),
      Seq(0, -1, 0),
      Seq(0, 0, 1)
    )
  }

  def rotate(angleDeg: Int): Matrix2D = {
    Matrix2D(
      Seq(IntMatrix.cos(angleDeg), -IntMatrix.sin(angleDeg), 0),
      Seq(IntMatrix.sin(angleDeg),  IntMatrix.cos(angleDeg), 0),
      Seq(0, 0, 1)
    )
  }

  /**
    * Translate usings the point's x and y values as the delta.  In other words,
    * translate such that a point at the origin would move to the given point.
    */
  private def fromOrigin(dxy: (Int, Int)): Matrix2D = translate(dxy._1, dxy._2)

  /**
    * @return a translation matrix that will move the point to the origin
    */
  private def toOrigin(point: (Int, Int)): Matrix2D = translate(-point._1, -point._2)

  /**
    * Make the given transform happen "at" the anchor point by first translating
    * the anchor point to the origin, performing the transform, and then translating
    * back to the anchor point.
    *
    * @param originTranslation
    * @param anchor
    * @return
    */
  private def at(originTransform: Matrix2D, anchor: (Int, Int)): Matrix2D = {
    inOrder(Seq(toOrigin(anchor), originTransform, fromOrigin(anchor)))
  }

  def flipXat(anchorX: Int): Matrix2D = at(flipX, (anchorX, 0))
  def flipYat(anchorY: Int): Matrix2D = at(flipY, (0, anchorY))

  def flipXY: Matrix2D = ??? // do we need this?
  def flipXYat(anchor: PointXY): Matrix2D = ???

  /** WARNING: for build engine need to flip CW and CCW */
  def rotateCCW: Matrix2D = rotate(90)

  /** WARNING: for build engine need to flip CW and CCW */
  def rotateCW: Matrix2D = rotate(270)

  /** WARNING: for build engine need to flip CW and CCW */
  def rotateAround(angleDeg: Int, anchor: PointXY): Matrix2D = at(rotate(angleDeg), anchor.toTuple)

  /** WARNING: for build engine need to flip CW and CCW */
  def rotateAroundCCW(anchor: PointXY): Matrix2D = rotateAround(90, anchor)

  /**
    * WARNING:  this is a "clockwise" rotation from classic a point of view where X+ goes to the right and Y+ goes
    * up.   Since the build engine has Y+ doing down, it is effectively flipped and you need to call CCW for CW
    * @param anchor
    * @return
    */
  def rotateAroundCW(anchor: PointXY): Matrix2D = rotateAround(270, anchor)

  def apply(row1: Seq[Int], row2: Seq[Int], row3: Seq[Int]): Matrix2D = {
    if(row1.length != 3 || row3.length != 3 || row3.length != 3) throw new IllegalArgumentException
    new Matrix2D(row1, row2, row3)
  }

  def apply(matrix: IntMatrix): Matrix2D = {
    if(matrix.rowCount != SIZE || matrix.colCount != SIZE) throw new IllegalArgumentException
    new Matrix2D(matrix.row(0), matrix.row(1), matrix.row(2))
  }

  /**
    * Creates a Matrix2D that will apply the transformations in the order specified here
    * (which actually requires multiplying them in the reverse order)
    */
  private def inOrder(transformations: Seq[Matrix2D]): Matrix2D = {
    Matrix2D(transformations.reverse.reduce(_ * _))
  }

}

/**
  * Need 3 rows for 2D.
  *
  */
class Matrix2D(row1: Seq[Int], row2: Seq[Int], row3: Seq[Int]) extends IntMatrix(Seq(row1, row2, row3)) {

  def *(point: PointXY): PointXY = {
    val t = this * (point.x, point.y)
    new PointXY(t._1, t._2)
  }

  def *(point: (Int, Int)): (Int, Int) = {
    val result = (this * IntMatrix.toColumn(Seq(point._1, point._2, 1))).col(0)
    require(result(2) == 1)
    (result(0), result(1))
  }

  def *(rh: Matrix2D): Matrix2D = {
    Matrix2D(super.*(rh))
  }

}
