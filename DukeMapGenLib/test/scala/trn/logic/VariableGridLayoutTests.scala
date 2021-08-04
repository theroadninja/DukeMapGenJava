package trn.logic

import org.junit.{Assert, Test}
import trn.PointXY
import trn.prefab.BoundingBox

class VariableGridLayoutTests {

  private def bb(xmin: Int, ymin: Int, xmax: Int, ymax: Int): BoundingBox = BoundingBox(xmin, ymin, xmax, ymax)

  @Test
  def testIndexing(): Unit = {

    val vg = VariableGridLayout(
      Seq(-3, -2, -1, 0, 1, 2, 3).map(i => i -> i * i).toMap,
      Seq(-1, 0, 1, 2, 3, 4).map(i => i -> i * i).toMap,
      0,
      0,
    )

    Assert.assertEquals(7, vg.colWidths.length)
    Assert.assertEquals(9, vg.colWidths(0))
    Assert.assertEquals(9, vg.colWidth(-3))
    Assert.assertEquals(4, vg.colWidths(1))
    Assert.assertEquals(1, vg.colWidths(2))
    Assert.assertEquals(0, vg.colWidths(3))
    Assert.assertEquals(0, vg.colWidth(0))
    Assert.assertEquals(1, vg.colWidths(4))
    Assert.assertEquals(4, vg.colWidths(5))
    Assert.assertEquals(9, vg.colWidths(6))
    Assert.assertEquals(9, vg.colWidth(3))
    Assert.assertEquals(-3, vg.colOffset)

    Assert.assertEquals(0, vg.colIndex(-3))
    Assert.assertEquals(1, vg.colIndex(-2))
    Assert.assertEquals(3, vg.colIndex(0))
    Assert.assertEquals(6, vg.colIndex(3))

    Assert.assertEquals(6, vg.rowHeights.length)
    Assert.assertEquals(1, vg.rowHeights(0))
    Assert.assertEquals(0, vg.rowHeights(1))
    Assert.assertEquals(1, vg.rowHeights(2))
    Assert.assertEquals(4, vg.rowHeights(3))
    Assert.assertEquals(9, vg.rowHeights(4))
    Assert.assertEquals(16, vg.rowHeights(5))
    Assert.assertEquals(-1, vg.rowOffset)

    Assert.assertEquals(0, vg.rowIndex(-1))
    Assert.assertEquals(1, vg.rowIndex(0))
    Assert.assertEquals(5, vg.rowIndex(4))
  }

  @Test
  def testSize(): Unit = {
    val vg = VariableGridLayout(
      Seq(3, 7, 13),
      Seq(1, 5, 11, 17),
      0,
      0,
      colSpace = 2,
      rowSpace = 10
    )
    Assert.assertEquals(3+2+7+2+13, vg.width)
    Assert.assertEquals(1+10+5+10+11+10+17, vg.height)
    Assert.assertEquals(BoundingBox(0, 0, 27, 64), vg.boundingBox)
    Assert.assertEquals(new PointXY(13, 32), vg.center)

    // top row
    Assert.assertEquals(bb(0, 0, 3, 1), vg.boundingBox(0, 0))
    Assert.assertEquals(bb(5, 0, 12, 1), vg.boundingBox(1, 0))
    Assert.assertEquals(bb(14, 0, 27, 1), vg.boundingBox(2, 0))

    // left column
    Assert.assertEquals(bb(0, 11, 3, 16), vg.boundingBox(0, 1))
    Assert.assertEquals(bb(0, 26, 3, 37), vg.boundingBox(0, 2))

    // last row
    Assert.assertEquals(bb(0, 47, 3, 64), vg.boundingBox(0, 3))
    Assert.assertEquals(bb(5, 47, 12, 64), vg.boundingBox(1, 3))
    Assert.assertEquals(bb(14, 47, 27, 64), vg.boundingBox(2, 3))
  }

}
