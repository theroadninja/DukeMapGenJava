package trn.math

import org.junit.{Assert, Test}

class SnapAngleTests {

  @Test
  def testMod(): Unit = {
    Assert.assertEquals(1, SnapAngle.modulo(1, 4))
    Assert.assertEquals(3, SnapAngle.modulo(-1, 4))

    Assert.assertEquals(SnapAngle(0), SnapAngle(0))
    Assert.assertNotEquals(SnapAngle(0), SnapAngle(1))
    Assert.assertNotEquals(SnapAngle(1), SnapAngle(-1))

    Assert.assertEquals(SnapAngle(0), SnapAngle(0))
    Assert.assertEquals(SnapAngle(1), SnapAngle(1))
    Assert.assertEquals(SnapAngle(2), SnapAngle(2))
    Assert.assertEquals(SnapAngle(3), SnapAngle(3))
    Assert.assertEquals(SnapAngle(4), SnapAngle(0))
    Assert.assertEquals(SnapAngle(5), SnapAngle(1))
    Assert.assertEquals(SnapAngle(6), SnapAngle(2))
    Assert.assertEquals(SnapAngle(7), SnapAngle(3))
    Assert.assertEquals(SnapAngle(8), SnapAngle(0))

    Assert.assertEquals(SnapAngle(0), SnapAngle(0))
    Assert.assertEquals(SnapAngle(-1), SnapAngle(3))
    Assert.assertEquals(SnapAngle(-2), SnapAngle(2))
    Assert.assertEquals(SnapAngle(-3), SnapAngle(1))
    Assert.assertEquals(SnapAngle(-4), SnapAngle(0))
    Assert.assertEquals(SnapAngle(-5), SnapAngle(3))
    Assert.assertEquals(SnapAngle(-6), SnapAngle(2))
    Assert.assertEquals(SnapAngle(-7), SnapAngle(1))
    Assert.assertEquals(SnapAngle(-8), SnapAngle(0))
  }

  @Test
  def testAdd(): Unit = {
    Assert.assertEquals(SnapAngle(1), SnapAngle(0) + SnapAngle(1))
    Assert.assertEquals(SnapAngle(1), SnapAngle(1) + SnapAngle(0))
    Assert.assertEquals(SnapAngle(2), SnapAngle(1) + SnapAngle(1))
    Assert.assertEquals(SnapAngle(0), SnapAngle(1) + SnapAngle(3))
    Assert.assertEquals(SnapAngle(1), SnapAngle(2) + SnapAngle(3))
    Assert.assertEquals(SnapAngle(3), SnapAngle(4) + SnapAngle(3))
    Assert.assertEquals(SnapAngle(3), SnapAngle(0) + SnapAngle(-1))
    Assert.assertEquals(SnapAngle(1), SnapAngle(2) + SnapAngle(-1))
  }

  @Test
  def testSubtract(): Unit = {
    Assert.assertEquals(SnapAngle(-1), SnapAngle(0) - SnapAngle(1))
    Assert.assertEquals(SnapAngle(1), SnapAngle(0) - SnapAngle(-1))

    Assert.assertEquals(SnapAngle(2), SnapAngle(0) - SnapAngle(2))
    Assert.assertEquals(SnapAngle(-3), SnapAngle(0) - SnapAngle(3))
    Assert.assertEquals(SnapAngle(1), SnapAngle(0) - SnapAngle(3))
    Assert.assertEquals(SnapAngle(0), SnapAngle(0) - SnapAngle(4))

    Assert.assertEquals(SnapAngle(1), SnapAngle(1) - SnapAngle(0))
    Assert.assertEquals(SnapAngle(0), SnapAngle(1) - SnapAngle(1))
    Assert.assertEquals(SnapAngle(3), SnapAngle(1) - SnapAngle(2))
    Assert.assertEquals(SnapAngle(2), SnapAngle(1) - SnapAngle(3))
    Assert.assertEquals(SnapAngle(1), SnapAngle(1) - SnapAngle(4))

    Assert.assertEquals(SnapAngle(2), SnapAngle(2) - SnapAngle(0))
    Assert.assertEquals(SnapAngle(1), SnapAngle(2) - SnapAngle(1))
    Assert.assertEquals(SnapAngle(0), SnapAngle(2) - SnapAngle(2))
    Assert.assertEquals(SnapAngle(3), SnapAngle(2) - SnapAngle(3))
    Assert.assertEquals(SnapAngle(2), SnapAngle(2) - SnapAngle(4))
    Assert.assertEquals(SnapAngle(1), SnapAngle(2) - SnapAngle(5))
  }

  @Test
  def testRotatedCW(): Unit = {
    Assert.assertEquals(SnapAngle(0), SnapAngle(-1).rotatedCW)
    Assert.assertEquals(SnapAngle(1), SnapAngle(0).rotatedCW)
    Assert.assertEquals(SnapAngle(2), SnapAngle(1).rotatedCW)
    Assert.assertEquals(SnapAngle(3), SnapAngle(2).rotatedCW)
    Assert.assertEquals(SnapAngle(0), SnapAngle(3).rotatedCW)

    Assert.assertEquals(SnapAngle(3), SnapAngle(0).rotatedCW.rotatedCW.rotatedCW)
  }

  @Test
  def testMultiple(): Unit = {
    // this operator is meant to be used on something like a SectorGroup.  Rotating another angle is just a silly way to
    // test (and for angles, probably works just like +)
    val a = SnapAngle(3)
    val b = SnapAngle(0)
    Assert.assertEquals(SnapAngle(3), a * b)

    Assert.assertEquals(SnapAngle(0), SnapAngle(0) * SnapAngle(0))
    Assert.assertEquals(SnapAngle(1), SnapAngle(0) * SnapAngle(1))
    Assert.assertEquals(SnapAngle(1), SnapAngle(1) * SnapAngle(0))
    Assert.assertEquals(SnapAngle(2), SnapAngle(1) * SnapAngle(1))
    Assert.assertEquals(SnapAngle(3), SnapAngle(2) * SnapAngle(1))
    Assert.assertEquals(SnapAngle(3), SnapAngle(1) * SnapAngle(2))
    Assert.assertEquals(SnapAngle(4), SnapAngle(2) * SnapAngle(2))
  }

}
