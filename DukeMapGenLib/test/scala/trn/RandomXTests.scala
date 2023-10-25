package trn

import org.junit.{Test, Assert}

class RandomXTests {

  @Test
  def testFlipCoin(): Unit = {
    val total = 100000
    val results = (0 to total).map { i =>
      new RandomX(i).flipCoin{"a"}{"b"}
    }
    val a = results.count(_ == "a").toFloat / total.toFloat
    val b = results.count(_ == "b").toFloat / total.toFloat
    Assert.assertTrue(a < 0.65)
    Assert.assertTrue(b < 0.65)
  }

  @Test
  def testRandomPairs(): Unit = {
    val r = new RandomX(0)
    val (pairs, leftovers) = r.randomUnequalPairs(Seq(1, 2, 3))
    Assert.assertEquals(1, leftovers.size)
    Assert.assertEquals(1, pairs.size)
    Assert.assertTrue(pairs.head._1 != pairs.head._2)

    val (pairs2, leftovers2) = r.randomUnequalPairs(Seq.empty)
    Assert.assertEquals(0, pairs2.size)
    Assert.assertEquals(0, leftovers2.size)

    val (pairs3, leftovers3) = r.randomUnequalPairs(Seq(1, 1, 1, 1))
    Assert.assertEquals(0, pairs3.size)
    Assert.assertEquals(4, leftovers3.size)

    val (pairs4, leftovers4) = r.randomUnequalPairs(Seq(1, 1, 1, 1, 1, 2, 2, 2, 2, 2))
    Assert.assertEquals(5, pairs4.size)
    pairs4.foreach { t =>
      Assert.assertNotEquals(t._1, t._2)
    }
    Assert.assertEquals(0, leftovers4.size)
  }

}
