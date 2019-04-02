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

  // TODO - create a more generic function simply called "at"
  def flipXat(anchorX: Int): Matrix2D = inOrder(Seq(translate(-anchorX, 0), flipX, translate(anchorX, 0)))

  def flipY: Matrix2D = {
    Matrix2D(
      Seq(1, 0, 0),
      Seq(0, -1, 0),
      Seq(0, 0, 1)
    )
  }

  def flipYat(anchorY: Int): Matrix2D = inOrder(Seq(translate(0, -anchorY), flipY, translate(0, anchorY)))

  def flipXY: Matrix2D = ??? // do we need this?
  def flipXYat(anchor: PointXY): Matrix2D = ???

  private def trig(map: Map[Int, Int])(degrees: Int): Int = {
    map.get(degrees % 360).getOrElse{
      throw new IllegalArgumentException("only right angles are supported")
    }
  }
  private def sin:Int => Int = trig(Map((0, 0), (90, 1), (180, 0), (270, -1)))
  private def cos:Int => Int = trig(Map((0, 1), (90, 0), (180, -1), (270, 0)))

  def rotate(angleDeg: Int): Matrix2D = {
    Matrix2D(
      Seq(cos(angleDeg), -sin(angleDeg), 0),
      Seq(sin(angleDeg),  cos(angleDeg), 0),
      Seq(0, 0, 1)
    )
  }

  def rotateAround(angleDeg: Int, anchor: PointXY): Matrix2D = ???

  def rotateCCW: Matrix2D = rotate(90)
  def rotateCW: Matrix2D = rotate(270)

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
    * @param transformations
    * @return
    */
  private def inOrder(transformations: Seq[Matrix2D]): Matrix2D = {
    Matrix2D(transformations.reverse.reduce(_ * _))
  }

}

/**
  * Need 3 rows for 2d.
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
