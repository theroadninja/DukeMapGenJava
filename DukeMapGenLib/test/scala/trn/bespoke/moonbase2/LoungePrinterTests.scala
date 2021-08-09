package trn.bespoke.moonbase2

import org.junit.{Assert, Test}
import trn.{BuildConstants, PointXY}
import trn.render.WallAnchor

class LoungePrinterTests {

  private def p(x: Int, y: Int): PointXY = new PointXY(x, y)
  private def anchor(p0: PointXY, p1: PointXY) = WallAnchor(p0, p1, BuildConstants.DefaultFloorZ, BuildConstants.DefaultCeilZ)

  @Test
  def testControlPoints(): Unit = {

    /**
      *    0, -1024
      *    /\
      * A  |
      *    0, 0
      *
      *                      4096, 4096
      *                          |       B
      *                         \/
      *                      4096, 5120
      */
    val results = LoungePrinter.controlPointsSimple(anchor(p(0, 0), p(0, -1024)), anchor(p(4096, 4096), p(4096, 5120)))
    val points: Seq[PointXY] = results.flatMap(_.controlPoints)
    Assert.assertEquals(Seq(p(0, -1024), p(4096, -1024), p(4096, 4096), p(4096, 5120), p(0, 5120), p(0, 0)), points)

    val sections: Seq[String] = results.flatMap(_.sections)
    Assert.assertEquals(Seq("EMPTY", "EMPTY", "ANCHOR", "EMPTY", "EMPTY", "ANCHOR"), sections)
  }

  /**
    *    0, -1024           2048,-1024
    *    /\                     |
    * A  |                     \/     B
    *    0, 0               2048, 0
    */
  @Test
  def testControlPoints2(): Unit = {
    val results = LoungePrinter.controlPointsSimple(anchor(p(0, 0), p(0, -1024)), anchor(p(2048, -1024), p(2048, 0)))
    val points: Seq[PointXY] = results.flatMap(_.controlPoints)
    Assert.assertEquals(Seq(p(0, -1024), p(2048, -1024), p(2048, 0), p(0, 0)), points)

    val sections: Seq[String] = results.flatMap(_.sections)
    Assert.assertEquals(Seq("EMPTY", "ANCHOR", "EMPTY", "ANCHOR"), sections)
  }

}
