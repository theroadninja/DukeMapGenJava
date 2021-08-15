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

}
