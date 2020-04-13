package trn.prefab

import org.junit.{Assert, Test}


class MapWriterTests {
  private lazy val testPalette: PrefabPalette = PrefabPalette.fromMap(TestUtils.load(TestUtils.MapWriterMap), true)

  @Test
  def testOrdinalHelpers(): Unit = {
    def allConns(groupId: Int): Seq[RedwallConnector] = testPalette.getSG(groupId).allRedwallConnectors

    def eastId(sgId: Int): Option[Int] = MapWriter.farthestEast(allConns(sgId)).map(_.getConnectorId)
    def westId(sgId: Int): Option[Int] = MapWriter.farthestWest(allConns(sgId)).map(_.getConnectorId)
    def northId(sgId: Int): Option[Int] = MapWriter.farthestNorth(allConns(sgId)).map(_.getConnectorId)
    def southId(sgId: Int): Option[Int] = MapWriter.farthestSouth(allConns(sgId)).map(_.getConnectorId)

    val EastId = 6
    val WestId = 4
    val NorthId = 8
    val SouthId = 2

    Assert.assertTrue(Seq(eastId(1), westId(1), southId(1), northId(1)).filter(_.isDefined).isEmpty)

    Seq(2, 3).foreach { g =>
      Assert.assertEquals(Some(EastId), eastId(g))
      Assert.assertTrue(Seq(westId(g), southId(g), northId(g)).filter(_.isDefined).isEmpty)
    }

    Seq(4, 5).foreach { g =>
      Assert.assertEquals(Some(NorthId), northId(g))
      Assert.assertTrue(Seq(eastId(g), westId(g), southId(g)).filter(_.isDefined).isEmpty)
    }

    Seq(6, 7).foreach { g =>
      Assert.assertEquals(Some(WestId), westId(g))
      Assert.assertTrue(Seq(eastId(g), northId(g), southId(g)).filter(_.isDefined).isEmpty)
    }

    Seq(8, 9).foreach { g =>
      Assert.assertEquals(Some(SouthId), southId(g))
      Assert.assertTrue(Seq(eastId(g), westId(g), northId(g)).filter(_.isDefined).isEmpty)
    }

    Assert.assertEquals(Some(EastId), eastId(10))
    Assert.assertEquals(Some(WestId), westId(10))
    Assert.assertEquals(Some(NorthId), northId(10))
    Assert.assertEquals(Some(SouthId), southId(10))

  }

}
