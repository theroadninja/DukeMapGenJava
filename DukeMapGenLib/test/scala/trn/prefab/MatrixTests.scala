package trn.prefab

import org.junit.{Assert, Test}
import trn.PointXY
import trn.PointXYImplicits._

class MatrixTests {

  @Test
  def rowcolTest(): Unit = {
    val m = IntMatrix(Seq(
      Seq(1, 2, 3),
      Seq(4, 5, 6),
    ))

    Assert.assertEquals(Seq(1, 2, 3), m.row(0))
    Assert.assertEquals(Seq(4, 5, 6), m.row(1))

    Assert.assertEquals(Seq(1, 4), m.col(0))
    Assert.assertEquals(Seq(2, 5), m.col(1))
    Assert.assertEquals(Seq(3, 6), m.col(2))
  }

  @Test
  def colAccessTest(): Unit = {
    val m = IntMatrix(Seq(
      Seq(1, 2, 3),
      Seq(4, 5, 6),
    ))

    val cols = m.cols
    Assert.assertEquals(3, cols.size)
    Assert.assertEquals(Seq(1, 4), cols(0))
    Assert.assertEquals(Seq(2, 5), cols(1))
    Assert.assertEquals(Seq(3, 6), cols(2))
  }

  @Test
  def multiplyVectorsTest(): Unit = {
    Assert.assertEquals(0, IntMatrix.multiply(Seq(0,0,0), Seq(0,0,0)))
    Assert.assertEquals(0, IntMatrix.multiply(Seq(1,1,1), Seq(0,0,0)))
    Assert.assertEquals(0, IntMatrix.multiply(Seq(0,0,0), Seq(1,1,1)))

    Assert.assertEquals(1, IntMatrix.multiply(Seq(1,0,0), Seq(1,0,0)))
    Assert.assertEquals(1, IntMatrix.multiply(Seq(0,1,0), Seq(1,1,0)))
    Assert.assertEquals(1, IntMatrix.multiply(Seq(1,0,1), Seq(0,1,1)))

    Assert.assertEquals(5, IntMatrix.multiply(Seq(1,2,0), Seq(1,2,3)))
    Assert.assertEquals(6, IntMatrix.multiply(Seq(1,2,3), Seq(1,1,1)))
    Assert.assertEquals(14, IntMatrix.multiply(Seq(1,2,3), Seq(1,2,3)))
    Assert.assertEquals(3+4+3, IntMatrix.multiply(Seq(3,2,1), Seq(1,2,3)))
  }

  @Test
  def equalsTest(): Unit = {
    val m = IntMatrix(Seq(
      Seq(1, 2),
      Seq(3, 4)
    ))
    val m1 = IntMatrix(Seq(
      Seq(1, 2),
      Seq(3, 4)
    ))
    val m2 = IntMatrix(Seq(
      Seq(1, 2),
      Seq(3, 4),
      Seq(5, 6)
    ))
    val m3 = IntMatrix(Seq(
      Seq(1, 5),
      Seq(3, 4)
    ))

    Assert.assertEquals(m, m)
    Assert.assertEquals(m, m1)

    Assert.assertNotEquals(m, m2)
    Assert.assertNotEquals(m, m3)
    Assert.assertNotEquals(m2, m3)

  }

  @Test
  def multiplyMatrix2x2Test(): Unit = {
    // TODO - test different sizes

    val m = IntMatrix(Seq(
      Seq(1, 7),
      Seq(2, 4)
    ))
    val m2 = IntMatrix(Seq(
      Seq(3, 1),
      Seq(5, 2)
    ))
    val expected1 = IntMatrix(Seq(
      Seq(3+35, 1+14),
      Seq(6+20, 2+8)
    ))
    val expected2 = IntMatrix(Seq(
      Seq(3+2, 21+4),
      Seq(5+4, 5*7+8)
    ))

    Assert.assertEquals(expected1, m * m2)
    Assert.assertEquals(expected2, m2 * m)
  }

  @Test
  def toColumnTest(): Unit = {
    val m = IntMatrix.toColumn(Seq(1, 2, 3))
    val expected = IntMatrix(Seq(
      Seq(1),
      Seq(2),
      Seq(3)
    ))
    Assert.assertEquals(expected, m)
  }

  @Test
  def multiplyMatrix2x1Test(): Unit = {
    val m = IntMatrix(Seq(
      Seq(1, 0),
      Seq(0, 1)
    ))
    val m2 = IntMatrix(Seq(
      Seq(4),
      Seq(5)
    ))

    //val m3 = IntMatrix.toColumn(new PointXY(4, 5).toSeq :+ 1)

    Assert.assertEquals(m2, m * m2)
  }

  @Test
  def multiplyMatrix3x1Test(): Unit = {
    val m = IntMatrix(Seq(
      Seq(1, 0, 0),
      Seq(0, 1, 0),
      Seq(0, 0, 1),
    ))
    val m2 = IntMatrix(Seq(
      Seq(1),
      Seq(2),
      Seq(3),
    ))
    Assert.assertEquals(m2, m * m2)
  }

}
