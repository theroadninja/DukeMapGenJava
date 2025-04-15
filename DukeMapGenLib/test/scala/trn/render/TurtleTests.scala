package trn.render

import org.junit.{Assert, Test}
import trn.{PointXY, FVectorXY}

import scala.util.Try

class TurtleTests {

  def p(x: Int, y: Int): PointXY = new PointXY(x, y)

  def v(x: Double, y: Double): FVectorXY = new FVectorXY(x, y)

  @Test
  def testInitialHeading(): Unit = {
    val points = Seq(p(0, 0), p(1, 0))
    val t = Turtle(points)
    Assert.assertTrue(v(1.0, 0.0).almostEquals(t.heading))

    val t2 = Turtle(Seq(p(2, 2), p(1, 2)))
    Assert.assertTrue(v(-1.0, 0.0).almostEquals(t2.heading))
  }

  @Test
  def testForwardStamp(): Unit = {
    val t = Turtle(Seq(p(0, 0), p(0, -1))) // remember this is NORTH
    t.forwardStamp(1.0)
    Assert.assertEquals(Seq(p(0, 0), p(0, -1), p(0, -2)), t.points.toSeq)

    t.turnRightD(90)
    t.forwardStamp(5.0)
    Assert.assertEquals(Seq(p(0, 0), p(0, -1), p(0, -2), p(5, -2)), t.points.toSeq)

    // dodecagon
    val d = Turtle(Seq(p(0, 0), p(0, 1)))
    for (_ <- 0 until 11){
      d.turnRightD(30) // 180 - 150
      d.forwardStamp(1.0)
    }
    Assert.assertEquals(p(0, 0), d.currentPos)
  }

  @Test
  def testOptions(): Unit = {
    val t = Turtle(Seq(p(0, 0), p(0, -1))) // remember this is NORTH
    val t2 = t.withGridSnap(32)
    Assert.assertTrue(t.options.gridSnap.isEmpty)
    Assert.assertEquals(32, t2.options.gridSnap.get)
  }

  @Test
  def testDuplicates(): Unit = {
    // start and end are same point (dodecagon)
    val d = Turtle(Seq(p(0, 0), p(0, 1)))
    for (_ <- 0 until 11) {
      d.turnRightD(30) // 180 - 150
      d.forwardStamp(1.0)
    }
    Assert.assertEquals(p(0, 0), d.currentPos)
    val t = Try(d.wallLoop())
    Assert.assertTrue(t.isFailure)

    // middle points overlap
    val turtle = Turtle(Seq(p(175, 60), p(1024, 512)))
    turtle.forwardStamp(0) // move nowhere, and add a point
    turtle.forwardStamp(256)
    Assert.assertTrue(Try(turtle.wallLoop()).isFailure)
  }

}
