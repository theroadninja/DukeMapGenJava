package trn.prefab.hypercube

import org.junit.Assert
import org.junit.Test
import trn.prefab.Heading
import scala.collection.JavaConverters._

class GridManagerV1Tests {

  val CellDist = 1025
  val ZW = (0 until 4).flatMap { z => (0 until 4).map { w => (z, w)}}

  @Test
  def testEdge(): Unit = {
    val gm = GridManager(CellDist, 4)

    ZW.foreach { case (z,w) =>
      Assert.assertTrue(gm.isEdgeXY((0, 0, z, w)))
      Assert.assertTrue(gm.isEdgeXY((0, 1, z, w)))
      Assert.assertTrue(gm.isEdgeXY((1, 0, z, w)))
      Assert.assertFalse(gm.isEdgeXY((1, 1, z, w)))
      Assert.assertFalse(gm.isEdgeXY((1, 2, z, w)))
      Assert.assertFalse(gm.isEdgeXY((2, 1, z, w)))
      Assert.assertFalse(gm.isEdgeXY((2, 2, z, w)))
      Assert.assertTrue(gm.isEdgeXY((2, 3, z, w)))
      Assert.assertTrue(gm.isEdgeXY((3, 2, z, w)))
      Assert.assertTrue(gm.isEdgeXY((3, 3, z, w)))
    }

    // 4 is out of bounds; not an edge
    Assert.assertFalse(gm.isEdgeXY((4, 4, 1, 1)))
  }

  @Test
  def testOutwardHeadings(): Unit = {
    val SideLength = 4
    val gm = GridManager(CellDist, SideLength)

    ZW.foreach { case (z, w) =>
      Assert.assertEquals(Seq(), gm.outwardHeadings((1, 1, z, w)).sorted)
      Assert.assertEquals(Seq(), gm.outwardHeadings((4, 4, z, w)).sorted)

      Assert.assertEquals(Seq(Heading.W, Heading.N), gm.outwardHeadings((0, 0, z, w)).sorted)
      Assert.assertEquals(Seq(Heading.N), gm.outwardHeadings((1, 0, z, w)).sorted)
      Assert.assertEquals(Seq(Heading.N), gm.outwardHeadings((2, 0, z, w)).sorted)
      Assert.assertEquals(Seq(Heading.E, Heading.N), gm.outwardHeadings((3, 0, z, w)).sorted)
      Assert.assertEquals(Seq(Heading.E), gm.outwardHeadings((3, 1, z, w)).sorted)
      Assert.assertEquals(Seq(Heading.E), gm.outwardHeadings((3, 2, z, w)).sorted)
      Assert.assertEquals(Seq(Heading.E, Heading.S), gm.outwardHeadings((3, 3, z, w)).sorted)
      Assert.assertEquals(Seq(Heading.S), gm.outwardHeadings((2, 3, z, w)).sorted)
      Assert.assertEquals(Seq(Heading.S), gm.outwardHeadings((1, 3, z, w)).sorted)
      Assert.assertEquals(Seq(Heading.S, Heading.W), gm.outwardHeadings((0, 3, z, w)).sorted)
      Assert.assertEquals(Seq(Heading.W), gm.outwardHeadings((0, 2, z, w)).sorted)
      Assert.assertEquals(Seq(Heading.W), gm.outwardHeadings((0, 1, z, w)).sorted)
    }
  }

  @Test
  def testMinRotationsToMatch(): Unit = {
    def f = GridManager.minRotationsToMatch _

    val All = Heading.all.asScala.map(_.toInt)
    All.foreach { currentHeading =>
      Assert.assertEquals(0, f(Heading.all.asScala.map(_.toInt), currentHeading))
    }

    Assert.assertEquals(3, f(Seq(), Heading.E))
    Assert.assertEquals(3, f(Seq(), Heading.S))
    Assert.assertEquals(3, f(Seq(), Heading.W))
    Assert.assertEquals(3, f(Seq(), Heading.N))

    Assert.assertEquals(0, f(Seq(Heading.E, Heading.S, Heading.W), Heading.E))
    Assert.assertEquals(0, f(Seq(Heading.E, Heading.S, Heading.W), Heading.S))
    Assert.assertEquals(0, f(Seq(Heading.E, Heading.S, Heading.W), Heading.W))
    Assert.assertEquals(1, f(Seq(Heading.E, Heading.S, Heading.W), Heading.N))

    Assert.assertEquals(0, f(Seq(Heading.E, Heading.W), Heading.E))
    Assert.assertEquals(1, f(Seq(Heading.E, Heading.W), Heading.S))
    Assert.assertEquals(0, f(Seq(Heading.E, Heading.W), Heading.W))
    Assert.assertEquals(1, f(Seq(Heading.E, Heading.W), Heading.N))

    Assert.assertEquals(1, f(Seq(Heading.S), Heading.E))
    Assert.assertEquals(0, f(Seq(Heading.S), Heading.S))
    Assert.assertEquals(3, f(Seq(Heading.S), Heading.W))
    Assert.assertEquals(2, f(Seq(Heading.S), Heading.N))
  }

  @Test
  def testEdgeRotationCount(): Unit = {
    val SideLength = 4
    val gm = GridManager(CellDist, SideLength)

    ZW.foreach { case (z, w) =>
      Assert.assertEquals(Seq(), gm.outwardHeadings((1, 1, z, w)).sorted)
      Assert.assertEquals(Seq(), gm.outwardHeadings((4, 4, z, w)).sorted)

      //W, N
      Assert.assertEquals(0, gm.edgeRotationCount((0, 0, z, w), Heading.N))
      Assert.assertEquals(0, gm.edgeRotationCount((0, 0, z, w), Heading.W))
      Assert.assertEquals(1, gm.edgeRotationCount((0, 0, z, w), Heading.S))
      Assert.assertEquals(2, gm.edgeRotationCount((0, 0, z, w), Heading.E))

      // N
      Assert.assertEquals(0, gm.edgeRotationCount((1, 0, z, w), Heading.N))
      Assert.assertEquals(1, gm.edgeRotationCount((1, 0, z, w), Heading.W))
      Assert.assertEquals(2, gm.edgeRotationCount((1, 0, z, w), Heading.S))
      Assert.assertEquals(3, gm.edgeRotationCount((1, 0, z, w), Heading.E))

      // E, N
      Assert.assertEquals(0, gm.edgeRotationCount((3, 0, z, w), Heading.E))
      Assert.assertEquals(1, gm.edgeRotationCount((3, 0, z, w), Heading.W))
      Assert.assertEquals(2, gm.edgeRotationCount((3, 0, z, w), Heading.S))
      Assert.assertEquals(0, gm.edgeRotationCount((3, 0, z, w), Heading.N))

      // W
      Assert.assertEquals(2, gm.edgeRotationCount((0, 2, z, w), Heading.E))
      Assert.assertEquals(0, gm.edgeRotationCount((0, 2, z, w), Heading.W))
      Assert.assertEquals(1, gm.edgeRotationCount((0, 2, z, w), Heading.S))
      Assert.assertEquals(3, gm.edgeRotationCount((0, 2, z, w), Heading.N))
    }

  }
}
