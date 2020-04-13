package trn.prefab.experiments

import org.junit.{Assert, Test}

class GridUtilTests {

  @Test
  def testAdj(): Unit = {
    // double check scala BS
    Assert.assertTrue(Seq(Seq(-1, 0)).toSet == Seq(Seq(-1, 0)).toSet)
    Assert.assertFalse(Seq(Seq(-1, 2)).toSet == Seq(Seq(-1, 0)).toSet)

    Assert.assertTrue(GridUtil.adj(0, 0).toSet == Seq(Seq(-1, 0), Seq(1, 0), Seq(0, -1), Seq(0, 1)).toSet)
    Assert.assertTrue(GridUtil.neighboors(0, 0).toSet == Seq((-1, 0), (1, 0), (0, -1), (0, 1)).toSet)

    Assert.assertTrue(GridUtil.adj(1, 2, 3).toSet == Seq(
      Seq(0, 2, 3), Seq(2, 2, 3),
      Seq(1, 1, 3), Seq(1, 3, 3),
      Seq(1, 2, 2), Seq(1, 2, 4),
    ).toSet)

    Assert.assertTrue(GridUtil.adj(-5, 3, 5, 7).toSet == Seq(
      Seq(-6, 3, 5, 7), Seq(-4, 3, 5, 7),
      Seq(-5, 2, 5, 7), Seq(-5, 4, 5, 7),
      Seq(-5, 3, 4, 7), Seq(-5, 3, 6, 7),
      Seq(-5, 3, 5, 6), Seq(-5, 3, 5, 8),
    ).toSet)
  }

}
