package duchy.vector

import org.junit.{Test, Assert}
import trn.PointXY

class VectorMathTests {
  val LineTypes = Seq(Line2D.LINE, Line2D.RAY, Line2D.SEGMENT)

  private def p(x: Int, y: Int): PointXY = new PointXY(x, y)

  @Test
  def testIntersectionParallel(): Unit = {
    Assert.assertFalse(
      VectorMath.intersection(
        Line2D.lineFromPoints(p(0, 0), p(1, 0)),
        Line2D.lineFromPoints(p(0, 5), p(10, 5)),
      ).isDefined
    )

    Assert.assertFalse(
      VectorMath.intersection(
        Line2D.lineFromPoints(p(0, 0), p(1, 0)),
        Line2D.lineFromPoints(p(0, 5), p(-10, 5)),
      ).isDefined
    )

    Assert.assertFalse(
      VectorMath.intersection(
        Line2D.lineFromPoints(p(1, 10), p(2, 11)),
        Line2D.lineFromPoints(p(1, 20), p(2, 21)),
      ).isDefined
    )
  }


  /**
    *  y
    *  /\
    *  |    /\
    *  |    |
    *  |    |
    * 5*----|----->
    *  |    |
    *  |    |
    *  +----*------------>  x
    *       5
    */
  @Test
  def testIntersectionHappyCase(): Unit = {
    val a = p(0, 5)
    val ab = p(10, 0)
    val c = p(5, 0)
    val cd = p(0, 10)

    LineTypes.foreach { line1type =>
      LineTypes.foreach { line2type =>
        Assert.assertEquals(
          p(5, 5),
          VectorMath.intersection(Line2D(a, ab, line1type), Line2D(c, cd, line2type)).get
        )
      }
    }
  }

  /**
    *                 +y
    *                 |
    *            /    |     \
    * -x <-----*------+------*------------>
    *                 |
    *                 |
    *                 -y (this is flipped from build)
    */
  @Test
  def testIntersectionCross(): Unit = {
    val a = p(-2, 0)
    val ab = p(1, 1)
    val c = p(2, 0)
    val cd = p(-1, 1)
    LineTypes.foreach { line1type =>
      LineTypes.foreach { line2type =>
        val result = VectorMath.intersection(Line2D(a, ab, line1type), Line2D(c, cd, line2type))
        if (line1type == Line2D.SEGMENT || line2type == Line2D.SEGMENT) {
          Assert.assertFalse(result.isDefined)
        } else {
          Assert.assertEquals(
            p(0, 2),
            result.get
          )
        }
      }
    }
  }

  /**
    *            /\
    *            |
    *            *
    *   *---___>
    */
  @Test
  def testIntersectionBehind(): Unit = {
    val a = p(10, 10)
    val ab = p(5, -1)
    val c = p(20, 10)
    val cd = p(0, 100)
    val line1 = Line2D(a, ab, Line2D.LINE)
    val line2 = Line2D(c, cd, Line2D.LINE)

    Assert.assertEquals(p(20, 8), VectorMath.intersection(line1, line2).get)
    Assert.assertEquals(p(20, 8), VectorMath.intersection(line1.asRay, line2).get)
    Assert.assertFalse(VectorMath.intersection(line1.asSegment, line2).isDefined)

    LineTypes.foreach { line1type =>
      Assert.assertFalse(VectorMath.intersection(line1.asType(line1type), line2.asRay).isDefined)
      Assert.assertFalse(VectorMath.intersection(line1.asType(line1type), line2.asSegment).isDefined)
    }
  }

}
