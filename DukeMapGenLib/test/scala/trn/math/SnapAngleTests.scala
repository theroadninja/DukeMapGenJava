package trn.math

import org.junit.{Assert, Test}
import trn.logic.Tile2d

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
  def testMultiply(): Unit = {
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

  @Test
  def testRotateUntil(): Unit = {
    val a = SnapAngle(0)
    val b = SnapAngle(2)
    val r = SnapAngle.rotateUntil(a){ x => x == b}.get
    Assert.assertEquals(SnapAngle(2), r)

    Assert.assertEquals(SnapAngle(0), SnapAngle.rotateUntil(a){ x => x == a}.get)
    Assert.assertFalse(SnapAngle.rotateUntil(a){ _ => false}.isDefined)

    val t = Tile2d(true, false, false, false)
    Assert.assertEquals(SnapAngle(0), SnapAngle.rotateUntil(t)(tile => tile.e == Tile2d.Conn).get)
    Assert.assertEquals(SnapAngle(1), SnapAngle.rotateUntil(t)(tile => tile.s == Tile2d.Conn).get)
    Assert.assertEquals(SnapAngle(2), SnapAngle.rotateUntil(t)(tile => tile.w == Tile2d.Conn).get)
    Assert.assertEquals(SnapAngle(3), SnapAngle.rotateUntil(t)(tile => tile.n == Tile2d.Conn).get)

    val t2 = Tile2d(false, false, false, true)
    Assert.assertEquals(SnapAngle(3), SnapAngle.rotateUntil(t)(tile => tile == t2).get)

    val t3 = Tile2d(false, true, false, true)
    Assert.assertFalse(SnapAngle.rotateUntil(t)(tile => tile == t3).isDefined)
  }

}
