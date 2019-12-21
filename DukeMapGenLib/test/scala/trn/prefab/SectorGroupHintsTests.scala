package trn.prefab

import org.junit.{Assert, Test}
import trn.prefab.hypercube.GridCell

import scala.collection.JavaConverters._

class SectorGroupHintsTests {
  val X = SectorGroupHints.HypercubeGridX
  val Y = SectorGroupHints.HypercubeGridY
  val Z = SectorGroupHints.HypercubeGridZ
  val W = SectorGroupHints.HypercubeGridW

  @Test
  def testHyperGridHintsCrossProduct(): Unit = {

    val x1 = PartialCell(Map(X -> 1))
    val y1 = PartialCell(Map(Y -> 1))

    val r1 = HypercubeGridHint.crossProduct(Seq(x1), Seq(y1))
    Assert.assertEquals(1, r1.size)
    Assert.assertEquals(r1(0), PartialCell(Map(X -> 1, Y -> 1)))

    val x2 = PartialCell(Map(X -> 2))
    val r2 = HypercubeGridHint.crossProduct(Seq(x1, x2), Seq(y1))
    Assert.assertEquals(2, r2.size)
    Assert.assertTrue(r2.contains(PartialCell(Map(X -> 1, Y -> 1))))
    Assert.assertTrue(r2.contains(PartialCell(Map(X -> 2, Y -> 1))))

    val y5 = PartialCell(Map(Y -> 5))
    val r3 = HypercubeGridHint.crossProduct(Seq(x1, x2), Seq(y1, y5))
    Assert.assertEquals(4, r3.size)
    println(r3)
    Assert.assertTrue(r3.contains(PartialCell(Map(X -> 1, Y -> 1))))
    Assert.assertTrue(r3.contains(PartialCell(Map(X -> 1, Y -> 5))))
    Assert.assertTrue(r3.contains(PartialCell(Map(X -> 2, Y -> 1))))
    Assert.assertTrue(r3.contains(PartialCell(Map(X -> 2, Y -> 5))))


    // val h1 = gh(X, 1)
    // val a = HypercubeGridHint.axisCrossProduct(Seq(h1))
    // Assert.assertEquals(1, a.size)
    // Assert.assertTrue(a(0).size == 1)
    // Assert.assertEquals(a(0)(0), gh(X, 1))

    // //Assert.assertArrayEquals(a, a)


    // //val h2 = gh(X, 2)

  }

  @Test
  def testHyperGridHintsCalculateCells(): Unit = {
    def gh(axis: Int, coord: Int) = HypercubeGridHint(axis, coord)

    val allHints: Seq[HypercubeGridHint] = Seq(
      gh(X, 1),
      gh(X, 2),
      gh(Y, 3),
      gh(Z, 0),
      gh(Z, 1),
      gh(W, 0)

    )

    Assert.assertEquals(0, HypercubeGridHint.calculateCells(Seq()).size)

    val normal = HypercubeGridHint.calculateCells(allHints)
    val noDuplicates = HypercubeGridHint.calculateCells(allHints ++ Seq(gh(X, 1), gh(X, 1)))
    Seq(normal, noDuplicates).foreach { result =>
      Assert.assertEquals(4, result.size)
      Assert.assertTrue(result.contains(PartialCell(Map(X -> 1, Y -> 3, Z -> 0, W -> 0))))
      Assert.assertTrue(result.contains(PartialCell(Map(X -> 1, Y -> 3, Z -> 1, W -> 0))))
      Assert.assertTrue(result.contains(PartialCell(Map(X -> 2, Y -> 3, Z -> 0, W -> 0))))
      Assert.assertTrue(result.contains(PartialCell(Map(X -> 2, Y -> 3, Z -> 1, W -> 0))))
    }
  }

  private def pc(elems: (Int, Int)*): PartialCell = PartialCell(Map(elems: _*))

  @Test
  def testPartialCellConflict(): Unit = {

    Assert.assertFalse(pc(X -> 1).conflicts(pc()))
    Assert.assertFalse(pc(X -> 1).conflicts(pc(Y -> 1)))
    Assert.assertFalse(pc(X -> 1).conflicts(pc(Y -> 2)))
    Assert.assertFalse(pc(X -> 1).conflicts(pc(X -> 1)))
    Assert.assertFalse(pc(X -> 1).conflicts(pc(X -> 1, Y -> 2, Z -> 3, W -> 4)))
    Assert.assertFalse(pc(X -> 1, Y -> 2, Z -> 3, W -> 4).conflicts(pc(X -> 1, Y -> 2, Z -> 3, W -> 4)))

    Assert.assertTrue(pc(X -> 1).conflicts(pc(X -> 2)))
    Assert.assertTrue(pc(X -> 1).conflicts(pc(X -> 2, Y -> 1, Z -> 1, W -> 1)))

    Assert.assertTrue(pc(X -> 9, Y -> 2, Z -> 3, W -> 4).conflicts(pc(X -> 1, Y -> 2, Z -> 3, W -> 4)))
    Assert.assertTrue(pc(X -> 1, Y -> 9, Z -> 3, W -> 4).conflicts(pc(X -> 1, Y -> 2, Z -> 3, W -> 4)))
    Assert.assertTrue(pc(X -> 1, Y -> 2, Z -> 9, W -> 4).conflicts(pc(X -> 1, Y -> 2, Z -> 3, W -> 4)))
    Assert.assertTrue(pc(X -> 1, Y -> 2, Z -> 3, W -> 9).conflicts(pc(X -> 1, Y -> 2, Z -> 3, W -> 4)))
  }

  @Test
  def testPartialCellUnion(): Unit = {
    Assert.assertEquals(PartialCell.EMPTY, PartialCell.EMPTY.union(PartialCell.EMPTY))

    Assert.assertEquals(pc(X -> 1), pc(X -> 1).union(pc(X -> 1)))
    Assert.assertEquals(PartialCell.EMPTY, pc(X -> 1).union(pc(X -> 2)))

    Assert.assertEquals(pc(X -> 1, Y -> 1), pc(X -> 1).union(pc(Y -> 1)))
    Assert.assertEquals(pc(X -> 1, Y -> 2), pc(X -> 1).union(pc(Y -> 2)))

    Assert.assertEquals(pc(X -> 1, Z -> 3, W -> 4), pc(X -> 1, Y -> 2, Z -> 3, W -> 4).union(pc(Y -> 1)))

  }

  @Test
  def testOtherCells(): Unit = {
    def gc(x: Int, y: Int, z: Int, w: Int) = GridCell.apply(x, y, z, w)
    val hint = SectorGroupHints.Empty

    Assert.assertEquals(Seq(), hint.otherCells(gc(0, 0, 0, 0)))
    Assert.assertEquals(Seq(), hint.copy(roomHeight = 1).otherCells(gc(0, 0, 0, 0)))
    Assert.assertEquals(Seq(gc(0, 0, 1, 0)), hint.copy(roomHeight = 2).otherCells(gc(0, 0, 0, 0)))
    Assert.assertEquals(Seq(gc(1, 2, 4, 4), gc(1, 2, 5, 4)), hint.copy(roomHeight = 3).otherCells(gc(1, 2, 3, 4)))

    Assert.assertEquals(Seq(gc(2, 0, 1, 0)), hint.copy(roomHeight = 2).otherCells(gc(2, 0, 0, 0)))
  }
}
