package trn.prefab

import scala.collection.mutable.ListBuffer
import org.junit.Assert
import org.junit.Test
import org.junit.Before

class BoundingBoxTests {

  @Test
  def testBoundingBox: Unit = {

    val bb = BoundingBox(0, 0, 10, 10)

    Assert.assertEquals(BoundingBox(0, 0, 10, 10), BoundingBox(0, 0, 10, 10).add(5, 5))

    // x min
    Assert.assertEquals(BoundingBox(4, 0, 10, 10), BoundingBox(4, 0, 10, 10).add(5, 5))
    Assert.assertEquals(BoundingBox(5, 0, 10, 10), BoundingBox(5, 0, 10, 10).add(5, 5))
    Assert.assertEquals(BoundingBox(5, 0, 10, 10), BoundingBox(6, 0, 10, 10).add(5, 5))

    // x max
    Assert.assertEquals(BoundingBox(0, 0, 10, 10), BoundingBox(0, 0, 10, 10).add(9, 5))
    Assert.assertEquals(BoundingBox(0, 0, 10, 10), BoundingBox(0, 0, 10, 10).add(10, 5))
    Assert.assertEquals(BoundingBox(0, 0, 11, 10), BoundingBox(0, 0, 10, 10).add(11, 5))

    // y min
    Assert.assertEquals(BoundingBox(0, 4, 10, 10), BoundingBox(0, 4, 10, 10).add(5, 5))
    Assert.assertEquals(BoundingBox(0, 5, 10, 10), BoundingBox(0, 5, 10, 10).add(5, 5))
    Assert.assertEquals(BoundingBox(0, 5, 10, 10), BoundingBox(0, 6, 10, 10).add(5, 5))

    // y max
    Assert.assertEquals(BoundingBox(0, 0, 10, 10), BoundingBox(0, 0, 10, 10).add(5, 9))
    Assert.assertEquals(BoundingBox(0, 0, 10, 10), BoundingBox(0, 0, 10, 10).add(5, 10))
    Assert.assertEquals(BoundingBox(0, 0, 10, 11), BoundingBox(0, 0, 10, 10).add(5, 11))

    // x min, y max
    Assert.assertEquals(BoundingBox(-1, 0, 10, 11), BoundingBox(0, 0, 10, 10).add(-1, 11))

    // x max, y min
    Assert.assertEquals(BoundingBox(0, -2, 12, 10), BoundingBox(0, 0, 10, 10).add(12, -2))
  }

  @Test
  def testFitsInside: Unit = {

    Assert.assertEquals(false, BoundingBox(0, 0, 10, 10).fitsInside(9, 9))
    Assert.assertEquals(false, BoundingBox(0, 0, 10, 10).fitsInside(9, 10))
    Assert.assertEquals(false, BoundingBox(0, 0, 10, 10).fitsInside(10, 9))
    Assert.assertEquals(false, BoundingBox(0, 0, 10, 10).fitsInside(9, 100))
    Assert.assertEquals(false, BoundingBox(0, 0, 10, 10).fitsInside(100, 9))

    Assert.assertEquals(true, BoundingBox(0, 0, 10, 10).fitsInside(10, 10))
    Assert.assertEquals(true, BoundingBox(0, 0, 10, 10).fitsInside(11, 10))
    Assert.assertEquals(true, BoundingBox(0, 0, 10, 10).fitsInside(10, 11))

    Assert.assertEquals(false, BoundingBox(0, 0, 10, 20).fitsInside(10, 10))
    Assert.assertEquals(false, BoundingBox(0, 0, 10, 20).fitsInside(15, 15))
    Assert.assertEquals(false, BoundingBox(0, 0, 10, 20).fitsInside(20, 15))
    Assert.assertEquals(false, BoundingBox(0, 0, 10, 20).fitsInside(21, 15))

    Assert.assertEquals(true, BoundingBox(0, 0, 10, 20).fitsInside(20, 20))
    Assert.assertEquals(true, BoundingBox(0, 0, 10, 20).fitsInside(21, 21))
  }


}
