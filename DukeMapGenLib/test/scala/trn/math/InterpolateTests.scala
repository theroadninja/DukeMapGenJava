package trn.math

import org.junit.{Assert, Test}
import trn.PointXY

class InterpolateTests {

  private def p(x: Int, y: Int): PointXY = new PointXY(x, y)

  @Test
  def testInterp: Unit = {
    Assert.assertEquals(p(0, 0), Interpolate.interpInt(p(0, 0), p(10, 0), 0))
    Assert.assertEquals(p(1, 0), Interpolate.interpInt(p(0, 0), p(10, 0), 0.1))
    Assert.assertEquals(p(3, 0), Interpolate.interpInt(p(0, 0), p(10, 0), 0.3))
    Assert.assertEquals(p(5, 0), Interpolate.interpInt(p(0, 0), p(10, 0), 0.5))
    Assert.assertEquals(p(9, 0), Interpolate.interpInt(p(0, 0), p(10, 0), 0.9))
    Assert.assertEquals(p(-9, 0), Interpolate.interpInt(p(0, 0), p(-10, 0), 0.9))
    Assert.assertEquals(p(10, 0), Interpolate.interpInt(p(0, 0), p(10, 0), 1))
    Assert.assertEquals(p(20, 0), Interpolate.interpInt(p(0, 0), p(10, 0), 2))

    Assert.assertEquals(p(0, 1), Interpolate.interpInt(p(0, 0), p(0, 10), 0.1))
    Assert.assertEquals(p(0, 3), Interpolate.interpInt(p(0, 0), p(0, 10), 0.3))
    Assert.assertEquals(p(0, 5), Interpolate.interpInt(p(0, 0), p(0, 10), 0.5))
    Assert.assertEquals(p(0, 9), Interpolate.interpInt(p(0, 0), p(0, 10), 0.9))
    Assert.assertEquals(p(0, 10), Interpolate.interpInt(p(0, 0), p(0, 10), 1))
    Assert.assertEquals(p(0, 20), Interpolate.interpInt(p(0, 0), p(0, 10), 2))

    Assert.assertEquals(p(5, 10), Interpolate.interpInt(p(0, 0), p(10, 20), 0.5))
    Assert.assertEquals(p(-5, 10), Interpolate.interpInt(p(0, 0), p(-10, 20), 0.5))
    Assert.assertEquals(p(-5, -10), Interpolate.interpInt(p(0, 0), p(-10, -20), 0.5))
  }

  @Test
  def testLinear: Unit = {
    Assert.assertTrue(Seq(p(0, 0), p(10, 0)) == Interpolate.linear(p(0, 0), p(10, 0), 2))
    Assert.assertTrue(Seq(p(0, 0), p(5, 0), p(10, 0)) == Interpolate.linear(p(0, 0), p(10, 0), 3))
    Assert.assertTrue(Seq(p(0, 0), p(4, 0), p(8, 0), p(12, 0)) == Interpolate.linear(p(0, 0), p(12, 0), 4))
    Assert.assertTrue(Seq(p(0, 0), p(25, 0), p(50, 0), p(75, 0), p(100, 0)) == Interpolate.linear(p(0, 0), p(100, 0), 5))
    Assert.assertTrue(Seq(p(42, 0), p(42, 25), p(42, 50), p(42, 75), p(42, 100)) == Interpolate.linear(p(42, 0), p(42, 100), 5))
  }

  // WRONG
  // @Test
  // def testQuad: Unit = {
  //   Assert.assertTrue(Seq(p(0, 0), p(10, 20)) == Interpolate.quad(p(0, 0), p(0, 10), p(10, 0), p(10, 20), 2))

  //   Assert.assertTrue(Seq(p(0, 0), p(5, 5), p(10, 10)) == Interpolate.quad(p(0, 0), p(0, 10), p(10, 0), p(10, 10), 3))
  //   val expected = Seq(p(0, 0), p(1, 1), p(2, 2), p(3, 3), p(4, 4), p(5, 5), p(6, 6), p(7, 7), p(8, 8), p(9, 9), p(10, 10))
  //   Assert.assertTrue(expected == Interpolate.quad(p(0, 0), p(0, 10), p(10, 0), p(10, 10), 11))
  // }

  @Test
  def testNormalize: Unit = {
    def toInt(v: (Double, Double)): PointXY = new PointXY(v._1.toInt, v._2.toInt)

    Assert.assertEquals(p(1, 0), toInt(Interpolate.normalize(1024.0, 0.0)))
    Assert.assertEquals(p(0, -1), toInt(Interpolate.normalize(0, -9.0)))
  }

}
