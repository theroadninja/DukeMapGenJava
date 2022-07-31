package trn.prefab.experiments.tiling

import org.junit.{Assert, Test}
import trn.math.SnapAngle
import trn.prefab.experiments.tiling.BigTileEdge._
import trn.prefab.experiments.tiling.SmallTileEdge._

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

  private def assertSeqEquals[T](seqA: Seq[T], seqB: Seq[T])(implicit arg0: math.Ordering[T]): Unit = {
    val a = seqA.sorted
    val b = seqB.sorted
    Assert.assertEquals(a.size, b.size)
    for(i <- 0 until a.size){
      Assert.assertEquals(a(i), b(i))
    }
  }

  @Test
  def testCalcEdgesBigTile(): Unit = {
    def f = PythagoreanTiling.calcEdges _
    Assert.assertTrue(f((0, 0), Seq.empty).isEmpty)
    assertSeqEquals(Seq(BigTileEdge.EB), f((0, 0), Seq((1, 0))))
    assertSeqEquals(Seq(BigTileEdge.ES), f((0, 0), Seq((1, 1))))
    assertSeqEquals(Seq.empty, f((0, 0), Seq((2, 1))))

    assertSeqEquals(Seq(EB, ES), f((0, 0), Seq((1, 0), (1, 1))))

    val allBig = Seq((1, 0), (0, 2), (-1, 0), (0, -2))
    assertSeqEquals(Seq(EB, SB, WB, NB), f((0, 0), allBig))

    val allSmall = Seq((1, 1), (0, 1), (0, -1), (1, -1))
    assertSeqEquals(Seq(ES, SS, WS, NS), f((0, 0), allSmall))

    assertSeqEquals(BigTileEdge.all, f((0, 0), allBig ++ allSmall))
  }

  @Test
  def testCalcEdgesSmallTile(): Unit = {
    def f = PythagoreanTiling.calcEdges _
    Assert.assertTrue(f((5, 5), Seq.empty).isEmpty)
    assertSeqEquals(Seq(E), f((5, 5), Seq((5, 6))))
    assertSeqEquals(Seq(S, W), f((5, 5), Seq((4, 6), (4, 4))))

    val allBig = Seq((5, 6), (4, 6), (4, 4), (5, 4))
    assertSeqEquals(SmallTileEdge.all, f((5, 5), allBig))
  }

  @Test
  def testRotationsToMatch(): Unit = {
    val E = SmallTileEdge.E
    val S = SmallTileEdge.S
    val W = SmallTileEdge.W
    val N = SmallTileEdge.N

    Assert.assertEquals(SnapAngle(0), PythagoreanTiling.rotationsToMatch(Seq(E), Seq(E), SmallTileEdge.rotateCW).get)
    Assert.assertEquals(SnapAngle(1), PythagoreanTiling.rotationsToMatch(Seq(E), Seq(S), SmallTileEdge.rotateCW).get)
    Assert.assertEquals(SnapAngle(2), PythagoreanTiling.rotationsToMatch(Seq(E), Seq(W), SmallTileEdge.rotateCW).get)
    Assert.assertEquals(SnapAngle(3), PythagoreanTiling.rotationsToMatch(Seq(E), Seq(N), SmallTileEdge.rotateCW).get)
    Assert.assertEquals(SnapAngle(1), PythagoreanTiling.rotationsToMatch(Seq(N), Seq(E), SmallTileEdge.rotateCW).get)
    Assert.assertEquals(SnapAngle(2), PythagoreanTiling.rotationsToMatch(Seq(N), Seq(S), SmallTileEdge.rotateCW).get)
    Assert.assertEquals(SnapAngle(3), PythagoreanTiling.rotationsToMatch(Seq(N), Seq(W), SmallTileEdge.rotateCW).get)
    Assert.assertEquals(SnapAngle(0), PythagoreanTiling.rotationsToMatch(Seq(N), Seq(N), SmallTileEdge.rotateCW).get)

    Assert.assertEquals(SnapAngle(0), PythagoreanTiling.rotationsToMatch(Seq(E, W), Seq(E, W), SmallTileEdge.rotateCW).get)
    Assert.assertEquals(SnapAngle(1), PythagoreanTiling.rotationsToMatch(Seq(E, W), Seq(N, S), SmallTileEdge.rotateCW).get)
    Assert.assertEquals(SnapAngle(1), PythagoreanTiling.rotationsToMatch(Seq(N, S), Seq(W, E), SmallTileEdge.rotateCW).get)
    Assert.assertEquals(SnapAngle(0), PythagoreanTiling.rotationsToMatch(Seq(E, N, W, S), Seq(E, S, W, N), SmallTileEdge.rotateCW).get)
  }

}
