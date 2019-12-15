package trn

import org.junit.{Assert, Test}

class MathUtilTests {

  @Test
  def testOverlaps(): Unit = {

    def _assertOverlaps(expected: Boolean, a0: Int, a1: Int, b0: Int, b1: Int): Unit ={
      Assert.assertEquals(expected, MathUtil.overlaps(a0, a1, b0, b1))
      Assert.assertEquals(expected, MathUtil.overlaps(a0, a1, b1, b0))
      Assert.assertEquals(expected, MathUtil.overlaps(a1, a0, b0, b1))
      Assert.assertEquals(expected, MathUtil.overlaps(a1, a0, b1, b0))
    }

    def assertOverlap(expected: Boolean, a0: Int, a1: Int, b0: Int, b1: Int): Unit ={
      _assertOverlaps(expected, a0, a1, b0, b1)
      _assertOverlaps(expected, b0, b1, a0, a1)
      _assertOverlaps(expected, -a0, -a1, -b0, -b1)
      _assertOverlaps(expected, -b0, -b1, -a0, -a1)
    }


    assertOverlap(false, 0, 0, 0, 0)
    assertOverlap(false, 0, 0, 0, 1)
    assertOverlap(false, 0, 0, 1, 2)
    assertOverlap(false, 0, 0, -1, 1)

    assertOverlap(true, 0, 1, 0, 1) // same segment
    assertOverlap(true, 0, 8, 0, 1) // one edge touches

    assertOverlap(false, 0, 0, 1, 0)
    assertOverlap(false, 0, 1, 1, 2)
    assertOverlap(false, -2, 1, 25, 30)
    assertOverlap(false, 0, 35, 35, 200)

    assertOverlap(true, 0, 2, 1, 2)
    assertOverlap(true, 0, 35, 10, 200)
    assertOverlap(true, 0, -2, -1, -2)
    assertOverlap(true, -10, 10, -1, 1) // one completely inside the other


    // if the points match up, but it still overlaps
    assertOverlap(true, 0, 1, 0, 1)
  }

}
