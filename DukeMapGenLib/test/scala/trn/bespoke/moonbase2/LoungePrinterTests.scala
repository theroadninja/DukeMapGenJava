package trn.bespoke.moonbase2

import org.junit.{Assert, Test}
import trn.prefab.Heading
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
    val points: Seq[PointXY] = results.allPoints
    Assert.assertEquals(Seq(p(4096, -1024), p(4096, 4096), p(4096, 5120), p(0, 5120), p(0, 0), p(0, -1024)), points)

    val sections: Seq[String] = results.allWallTypes
    Assert.assertEquals(Seq("EMPTY", "ANCHOR", "EMPTY", "EMPTY", "ANCHOR", "EMPTY"), sections)

    Assert.assertEquals(Seq(p(4096, -1024), p(4096, 4096), p(4096, 5120)), results.allEastPoints)
    Assert.assertEquals(Seq(p(4096, 5120), p(0, 5120)), results.allSouthPoints)
    Assert.assertEquals(Seq(p(0, 5120), p(0, 0), p(0, -1024)), results.allWestPoints)
    Assert.assertEquals(Seq(p(0, -1024), p(4096, -1024)), results.allNorthPoints)

    Assert.assertEquals(results.allEastPoints, results.points(Heading.E))
    Assert.assertEquals(results.allSouthPoints, results.points(Heading.S))
    Assert.assertEquals(results.allWestPoints, results.points(Heading.W))
    Assert.assertEquals(results.allNorthPoints, results.points(Heading.N))
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
    val points: Seq[PointXY] = results.allPoints
    Assert.assertEquals(Seq(p(2048, -1024), p(2048, 0), p(0, 0), p(0, -1024)), points)

    val sections: Seq[String] = results.allWallTypes
    Assert.assertEquals(Seq("ANCHOR", "EMPTY", "ANCHOR", "EMPTY"), sections)
  }

  /**
    *
    *                4096,-1792 --> 5120,-1792
    *
    *    256, 0
    *    /\
    * A  |
    *    256, 2048
    */
  @Test
  def testControlPoints3(): Unit = {
    val results = LoungePrinter.controlPointsSimple(anchor(p(256, 2048), p(256, 0)), anchor(p(4096, -1792), p(5120, -1792)))
    val points: Seq[PointXY] = results.allPoints
    Assert.assertEquals(Seq(p(5120, -1792), p(5120, 2048), p(256, 2048), p(256, 0), p(256, -1792), p(4096, -1792)), points)
    val sections: Seq[String] = results.allWallTypes
    Assert.assertEquals(Seq(LoungeWall.Empty, LoungeWall.Empty, LoungeWall.Anchor, LoungeWall.Empty, LoungeWall.Empty, LoungeWall.Anchor), sections)

  }

  @Test
  def testChairControlPoints(): Unit = {
    val results2 = LoungePrinter.chairControlPoints(p(0, 0), p(1000000, 0), 2)

    Assert.assertEquals(Seq(p(0, 0), p(0, 128), p(0, 384), p(512, 384), p(512, 128), p(512, 0)), results2)

  }

}
