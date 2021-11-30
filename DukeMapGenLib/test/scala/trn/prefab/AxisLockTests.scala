package trn.prefab

import org.junit.{Assert, Test}

class AxisLockTests {
  val X = 0
  val Y = 1
  val Z = 2
  val W = 3

  @Test
  def testAxisLock(): Unit = {

    Assert.assertEquals(AxisLock(X, 0), AxisLock(0))
    Assert.assertEquals(AxisLock(X, 5), AxisLock(5))
    Assert.assertEquals(AxisLock(X, 15), AxisLock(15))

    Assert.assertEquals(AxisLock(Y, 0), AxisLock(16))
    Assert.assertEquals(AxisLock(Y, 1), AxisLock(17))
    Assert.assertEquals(AxisLock(Y, 15), AxisLock(31))

    Assert.assertEquals(AxisLock(Z, 0), AxisLock(32))
    Assert.assertEquals(AxisLock(Z, 15), AxisLock(47))

    Assert.assertEquals(AxisLock(W, 0), AxisLock(48))
    Assert.assertEquals(AxisLock(W, 15), AxisLock(63))
  }

  @Test
  def testMatches(): Unit = {
    Assert.assertTrue(AxisLock(X, 0).matches(0, 0, 0, 0))
    Assert.assertTrue(AxisLock(X, 5).matches(5, 0, 0, 0))
    Assert.assertTrue(AxisLock(X, 0).matches(0, 1, 1, 1))
    Assert.assertFalse(AxisLock(X, 0).matches(1, 0, 0, 0))

    Assert.assertTrue(AxisLock(Y, 7).matches(0, 7, 0, 0))
    Assert.assertFalse(AxisLock(Y, 7).matches(7, 0, 7, 7))

    Assert.assertTrue(AxisLock(Z, 7).matches(0, 0, 7, 0))
    Assert.assertFalse(AxisLock(Z, 7).matches(7, 7, 0, 7))

    Assert.assertTrue(AxisLock(W, 7).matches(0, 0, 0, 7))
    Assert.assertFalse(AxisLock(W, 7).matches(7, 7, 7, 0))
  }

  @Test
  def testMatchAll(): Unit = {
    val locks = Seq(
      AxisLock(X, 5),
      AxisLock(Y, 5),
    )

    Assert.assertTrue(AxisLock.matchAll(locks, 5, 5, 0, 0))
    Assert.assertFalse(AxisLock.matchAll(locks, 0, 5, 0, 0))
    Assert.assertFalse(AxisLock.matchAll(locks, 5, 0, 0, 0))
    Assert.assertFalse(AxisLock.matchAll(locks, 0, 0, 0, 0))
  }

  @Test
  def testMatchOr(): Unit = {
    val locks = Seq(
      AxisLock(X, 2),
      AxisLock(Y, 2),
      AxisLock(Z, 1),
      AxisLock(Z, 2),
    )

    for(w <- 0 to 2){
      Assert.assertFalse(AxisLock.matchAll(locks, 2, 2, 0, w))
      Assert.assertTrue(AxisLock.matchAll(locks, 2, 2, 1, w))
      Assert.assertTrue(AxisLock.matchAll(locks, 2, 2, 2, w))

      Assert.assertFalse(AxisLock.matchAll(locks, 1, 2, 1, w))
      Assert.assertFalse(AxisLock.matchAll(locks, 2, 1, 1, w))
    }


  }

}
