package trn.prefab

import org.junit.Assert
import org.junit.Test
import trn.PointXY

class Matrix2DTests {

  def p(tuple: (Int, Int)): PointXY = new PointXY(tuple._1, tuple._2)

  def toPoints(pairs: Seq[((Int, Int), (Int, Int))]): Seq[(PointXY, PointXY)] = pairs.map {
    case (p1, p2) => (p(p1), p(p2))
  }

  @Test
  def testFlipX(): Unit = {
    val pairs = toPoints(Seq(
      ((-5, 0), (5, 0)),
      ((-5, 1), (5, 1)),
      ((-5, -1), (5, -1)),
      ((-5, 5), (5, 5)),
      ((-5, -5), (5, -5)),
      ((-5, 6), (5, 6)),
      ((1, 1), (-1, 1)),
      ((10, 5), (-10, 5))
    ))

    pairs.foreach { case (p1, p2) =>
      Assert.assertEquals(p1, Matrix2D.flipX * p2)
      Assert.assertEquals(p2, Matrix2D.flipX * p1)
    }
  }

  @Test
  def testFlipY(): Unit = {
    val pairs = toPoints(Seq(
      ((0, 5), (0, -5)),
      ((1, 5), (1, -5)),
      ((-1, 5), (-1, -5)),
      ((1, 1), (1, -1)),
      ((2, 2), (2, -2))
    ))

    pairs.foreach { t =>
      Assert.assertEquals(t._1, Matrix2D.flipY * t._2)
      Assert.assertEquals(t._2, Matrix2D.flipY * t._1)
    }
  }

  @Test
  def translateTests(): Unit = {
    Assert.assertEquals(new PointXY(1, 1), Matrix2D.translate(1, 1) * new PointXY(0, 0))
    Assert.assertEquals(new PointXY(1, 0), Matrix2D.translate(1, 0) * new PointXY(0, 0))
    Assert.assertEquals(new PointXY(1, -1), Matrix2D.translate(1, -1) * new PointXY(0, 0))
    Assert.assertEquals(new PointXY(0, -1), Matrix2D.translate(0, -1) * new PointXY(0, 0))
    Assert.assertEquals(new PointXY(-1, -1), Matrix2D.translate(-1, -1) * new PointXY(0, 0))
    Assert.assertEquals(new PointXY(-1, 0), Matrix2D.translate(-1, 0) * new PointXY(0, 0))
    Assert.assertEquals(new PointXY(-1, 1), Matrix2D.translate(-1, 1) * new PointXY(0, 0))
    Assert.assertEquals(new PointXY(0, 1), Matrix2D.translate(0, 1) * new PointXY(0, 0))

    Assert.assertEquals(new PointXY(3, 3), Matrix2D.translate(1, 1) * new PointXY(2, 2))
    Assert.assertEquals(new PointXY(3, 2), Matrix2D.translate(1, 0) * new PointXY(2, 2))
    Assert.assertEquals(new PointXY(3, 1), Matrix2D.translate(1, -1) * new PointXY(2, 2))
    Assert.assertEquals(new PointXY(2, 1), Matrix2D.translate(0, -1) * new PointXY(2, 2))
    Assert.assertEquals(new PointXY(1, 1), Matrix2D.translate(-1, -1) * new PointXY(2, 2))
    Assert.assertEquals(new PointXY(1, 2), Matrix2D.translate(-1, 0) * new PointXY(2, 2))
    Assert.assertEquals(new PointXY(1, 3), Matrix2D.translate(-1, 1) * new PointXY(2, 2))
    Assert.assertEquals(new PointXY(2, 3), Matrix2D.translate(0, 1) * new PointXY(2, 2))

    Assert.assertEquals(new PointXY(-4, -4), Matrix2D.translate(1, 1) * new PointXY(-5, -5))
    Assert.assertEquals(new PointXY(-7, -6), Matrix2D.translate(-2, -1) * new PointXY(-5, -5))
    Assert.assertEquals(new PointXY(1, 4), Matrix2D.translate(6, 9) * new PointXY(-5, -5))

    Assert.assertEquals(new PointXY(13, 42-17), Matrix2D.translate(1, -17) * new PointXY(12, 42))
    Assert.assertEquals(new PointXY(13, 42+17), Matrix2D.translate(1, 17) * new PointXY(12, 42))
    Assert.assertEquals(new PointXY(-11, 42-17), Matrix2D.translate(1, -17) * new PointXY(-12, 42))
    Assert.assertEquals(new PointXY(-11, 42+17), Matrix2D.translate(1, 17) * new PointXY(-12, 42))
  }

  @Test
  def flipXanchorTests(): Unit = {
    // flip around x=0
    for(x <- -42 to 42; y <- -42 to 42){
      val expected = new PointXY(-x, y)
      Assert.assertEquals(expected, Matrix2D.flipXat(0) * new PointXY(x, y))
    }

    // flip around x=5
    for(y <- -42 to 42){
      Assert.assertEquals(new PointXY(47+5, y), Matrix2D.flipXat(5) * new PointXY(-42, y))
      Assert.assertEquals(new PointXY(11, y), Matrix2D.flipXat(5) * new PointXY(-1, y))
      Assert.assertEquals(new PointXY(10, y), Matrix2D.flipXat(5) * new PointXY(0, y))
      Assert.assertEquals(new PointXY(9, y), Matrix2D.flipXat(5) * new PointXY(1, y))
      Assert.assertEquals(new PointXY(6, y), Matrix2D.flipXat(5) * new PointXY(4, y))
      Assert.assertEquals(new PointXY(5, y), Matrix2D.flipXat(5) * new PointXY(5, y))
      Assert.assertEquals(new PointXY(4, y), Matrix2D.flipXat(5) * new PointXY(6, y))
      Assert.assertEquals(new PointXY(0, y), Matrix2D.flipXat(5) * new PointXY(10, y))
      Assert.assertEquals(new PointXY(-2, y), Matrix2D.flipXat(5) * new PointXY(12, y))
    }

    // flip around x=-16
    for(y <- -42 to 42){
      Assert.assertEquals(new PointXY(-16, y), Matrix2D.flipXat(-16) * new PointXY(-16, y))
      Assert.assertEquals(new PointXY(-15, y), Matrix2D.flipXat(-16) * new PointXY(-17, y))
      Assert.assertEquals(new PointXY(-17, y), Matrix2D.flipXat(-16) * new PointXY(-15, y))
      Assert.assertEquals(new PointXY(-31, y), Matrix2D.flipXat(-16) * new PointXY(-1, y))
      Assert.assertEquals(new PointXY(-1, y), Matrix2D.flipXat(-16) * new PointXY(-31, y))
      Assert.assertEquals(new PointXY(-32, y), Matrix2D.flipXat(-16) * new PointXY(0, y))
      Assert.assertEquals(new PointXY(0, y), Matrix2D.flipXat(-16) * new PointXY(-32, y))
      Assert.assertEquals(new PointXY(-33, y), Matrix2D.flipXat(-16) * new PointXY(1, y))
      Assert.assertEquals(new PointXY(1, y), Matrix2D.flipXat(-16) * new PointXY(-33, y))
      Assert.assertEquals(new PointXY(-48, y), Matrix2D.flipXat(-16) * new PointXY(16, y))
    }
  }

  @Test
  def flipYanchorTests(): Unit = {
    // flip around x=0
    for(y <- -42 to 42; x <- -42 to 42){
      Assert.assertEquals((x, -y), Matrix2D.flipYat(0) * (x, y))
    }

    // flip around x=5
    for(x <- -42 to 42){
      Seq(
        (-42, 47+5),
        (11, -1),
        (10, 0),
        (9, 1),
        (6, 4),
        (5, 5),
      ).foreach{ case (y1: Int, y2: Int) =>
        Assert.assertEquals((x, y1), Matrix2D.flipYat(5) * (x, y2))
        Assert.assertEquals((x, y2), Matrix2D.flipYat(5) * (x, y1))
      }
    }

    // // flip around x=-16
    for(x <- -42 to 42){
      Seq(
        (-16, -16),
        (-15, -17),
        (-31, -1),
        (-32, 0),
        (-33, 1),
        (-48, 16),
      ).foreach{ case (y1: Int, y2: Int) =>
        Assert.assertEquals((x, y1), Matrix2D.flipYat(-16) * (x, y2))
        Assert.assertEquals((x, y2), Matrix2D.flipYat(-16) * (x, y1))
      }
    }
  }

  @Test
  def rotateTest(): Unit = {
    val m = Matrix2D.rotate(0)
    val m180 = Matrix2D.rotate(180)
    val m360 = Matrix2D.rotate(360)
    for(x <- -42 to 42; y <- -42 to 42){
      Assert.assertEquals((x, y), m * (x, y))
      Assert.assertEquals((x, y), m180 * (m180 * (x, y)))
      Assert.assertEquals((x, y), m180 * m180 * (x, y))
      Assert.assertEquals((x, y), m360 * (x, y))
    }
  }

  @Test
  def rotateTestsCCW(): Unit = {
    // TODO - a rotate 0
    val m = Matrix2D.rotateCCW
    Assert.assertEquals((0, 1), m * (1, 0))
    Assert.assertEquals((-1, 2), m * (2, 1))
    Assert.assertEquals((-1, 1), m * (1, 1))
    Assert.assertEquals((-2, 1), m * (1, 2))
    Assert.assertEquals((-3, 0), m * (0, 3))
    Assert.assertEquals((-3, -1), m * (-1, 3))
    Assert.assertEquals((-1, -1), m * (-1, 1))
    Assert.assertEquals((0, -2), m * (-2, 0))
    Assert.assertEquals((1, -2), m * (-2, -1))
    Assert.assertEquals((5, -5), m * (-5, -5))
    Assert.assertEquals((5, -1), m * (-1, -5))
    Assert.assertEquals((6, 0), m * (0, -6))
    Assert.assertEquals((6, 1), m * (1, -6))
    Assert.assertEquals((3, 4), m * (4, -3))
  }

  @Test
  def rotateTestsCW(): Unit = {
    val m = Matrix2D.rotateCW
    val points = Seq(
      ((1, 0), (0, -1)),
      ((2, -1), (-1, -2)),
      ((3, -3), (-3, -3)),
      ((1, -5), (-5, -1)),
      ((0, -6), (-6, 0)),
      ((-1, -7), (-7, 1)),
      ((-8, -8), (-8, 8)),
      ((-9, -1), (-1, 9)),
      ((-10, 0), (0, 10)),
      ((-11, 2), (2, 11)),
      ((-12, 12), (12, 12)),
      ((-3, 13), (13, 3)),
      ((0, 14), (14, 0)),
      ((1, 15), (15, -1)),
      ((16, 16), (16, -16)),
      ((17, 1), (1, -17))
    )
    points.foreach { case (p1: (Int, Int), p2: (Int, Int)) =>
      Assert.assertEquals(p2, m * p1)
      Assert.assertEquals(p2, Matrix2D.rotateCCW * Matrix2D.rotate(180) * p1)
      Assert.assertEquals(p2, m * m * m * m * m * p1)
    }
  }

}
