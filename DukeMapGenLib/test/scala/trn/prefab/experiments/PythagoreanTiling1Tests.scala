package trn.prefab.experiments

import org.junit.{Assert, Test}
import trn.math.SnapAngle

class PythagoreanTiling1Tests {

  @Test
  def testBigEdge(): Unit = {
    Assert.assertEquals(BigTileEdge.EB, BigTileEdge.edge((0, 0), (1, 0)).get)
    Assert.assertEquals(BigTileEdge.SB, BigTileEdge.edge((0, 0), (0, 2)).get)
    Assert.assertEquals(BigTileEdge.WB, BigTileEdge.edge((0, 0), (-1, 0)).get)
    Assert.assertEquals(BigTileEdge.NB, BigTileEdge.edge((0, 0), (0, -2)).get)

    Assert.assertEquals(BigTileEdge.ES, BigTileEdge.edge((10, 10), (11, 11)).get)
    Assert.assertEquals(BigTileEdge.SS, BigTileEdge.edge((10, 10), (10, 11)).get)
    Assert.assertEquals(BigTileEdge.WS, BigTileEdge.edge((10, 10), (10, 9)).get)
    Assert.assertEquals(BigTileEdge.NS, BigTileEdge.edge((10, 10), (11, 9)).get)
  }

  @Test
  def testSmallEdge(): Unit = {
    Assert.assertEquals(SmallTileEdge.E, SmallTileEdge.edge((5, 5), (5, 6)).get)
    Assert.assertEquals(SmallTileEdge.S, SmallTileEdge.edge((5, 5), (4, 6)).get)
    Assert.assertEquals(SmallTileEdge.W, SmallTileEdge.edge((5, 5), (4, 4)).get)
    Assert.assertEquals(SmallTileEdge.N, SmallTileEdge.edge((5, 5), (5, 4)).get)
  }

  @Test
  def testRotateSmallEdge(): Unit = {
    Assert.assertEquals(SmallTileEdge.S, SmallTileEdge.rotateCW(SmallTileEdge.E))
    Assert.assertEquals(SmallTileEdge.W, SmallTileEdge.rotateCW(SmallTileEdge.S))
    Assert.assertEquals(SmallTileEdge.N, SmallTileEdge.rotateCW(SmallTileEdge.W))
    Assert.assertEquals(SmallTileEdge.E, SmallTileEdge.rotateCW(SmallTileEdge.N))
  }

  @Test
  def testSmallRotate(): Unit = {
    Assert.assertEquals(SnapAngle(0), SmallTileEdge.rotationToMatch(SmallTileEdge.E, SmallTileEdge.E))
    Assert.assertEquals(SnapAngle(1), SmallTileEdge.rotationToMatch(SmallTileEdge.E, SmallTileEdge.S))
    Assert.assertEquals(SnapAngle(2), SmallTileEdge.rotationToMatch(SmallTileEdge.E, SmallTileEdge.W))
    Assert.assertEquals(SnapAngle(3), SmallTileEdge.rotationToMatch(SmallTileEdge.E, SmallTileEdge.N))

    Assert.assertEquals(SnapAngle(0), SmallTileEdge.rotationToMatch(SmallTileEdge.S, SmallTileEdge.S))
    Assert.assertEquals(SnapAngle(1), SmallTileEdge.rotationToMatch(SmallTileEdge.S, SmallTileEdge.W))
    Assert.assertEquals(SnapAngle(2), SmallTileEdge.rotationToMatch(SmallTileEdge.S, SmallTileEdge.N))
    Assert.assertEquals(SnapAngle(3), SmallTileEdge.rotationToMatch(SmallTileEdge.S, SmallTileEdge.E))

    Assert.assertEquals(SnapAngle(0), SmallTileEdge.rotationToMatch(SmallTileEdge.W, SmallTileEdge.W))
    Assert.assertEquals(SnapAngle(1), SmallTileEdge.rotationToMatch(SmallTileEdge.W, SmallTileEdge.N))
    Assert.assertEquals(SnapAngle(2), SmallTileEdge.rotationToMatch(SmallTileEdge.W, SmallTileEdge.E))
    Assert.assertEquals(SnapAngle(3), SmallTileEdge.rotationToMatch(SmallTileEdge.W, SmallTileEdge.S))

    Assert.assertEquals(SnapAngle(0), SmallTileEdge.rotationToMatch(SmallTileEdge.N, SmallTileEdge.N))
    Assert.assertEquals(SnapAngle(1), SmallTileEdge.rotationToMatch(SmallTileEdge.N, SmallTileEdge.E))
    Assert.assertEquals(SnapAngle(2), SmallTileEdge.rotationToMatch(SmallTileEdge.N, SmallTileEdge.S))
    Assert.assertEquals(SnapAngle(3), SmallTileEdge.rotationToMatch(SmallTileEdge.N, SmallTileEdge.W))
  }

  @Test
  def testSmallOpposite(): Unit = {
    Assert.assertTrue(SmallTileEdge.opposite(SmallTileEdge.E, SmallTileEdge.W))
    Assert.assertTrue(SmallTileEdge.opposite(SmallTileEdge.S, SmallTileEdge.N))
    Assert.assertTrue(SmallTileEdge.opposite(SmallTileEdge.W, SmallTileEdge.E))
    Assert.assertTrue(SmallTileEdge.opposite(SmallTileEdge.N, SmallTileEdge.S))

    Assert.assertFalse(SmallTileEdge.opposite(SmallTileEdge.E, SmallTileEdge.E))
    Assert.assertFalse(SmallTileEdge.opposite(SmallTileEdge.E, SmallTileEdge.S))
    Assert.assertFalse(SmallTileEdge.opposite(SmallTileEdge.E, SmallTileEdge.N))

    Assert.assertFalse(SmallTileEdge.opposite(SmallTileEdge.S, SmallTileEdge.S))
    Assert.assertFalse(SmallTileEdge.opposite(SmallTileEdge.S, SmallTileEdge.W))
    Assert.assertFalse(SmallTileEdge.opposite(SmallTileEdge.S, SmallTileEdge.E))

    Assert.assertFalse(SmallTileEdge.opposite(SmallTileEdge.N, SmallTileEdge.N))
    Assert.assertFalse(SmallTileEdge.opposite(SmallTileEdge.N, SmallTileEdge.E))
    Assert.assertFalse(SmallTileEdge.opposite(SmallTileEdge.N, SmallTileEdge.W))
  }

}
