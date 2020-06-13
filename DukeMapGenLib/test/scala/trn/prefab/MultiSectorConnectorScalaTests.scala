package trn.prefab

import org.junit.{Assert, Test}
import trn.PointXYZ

/**
  * TODO - there is also a MultiSectorConnectorTests.java ...
  */
class MultiSectorConnectorScalaTests {

  lazy val palette: PrefabPalette = PrefabPalette.fromMap(TestUtils.load(TestUtils.MultiSect), true);

  @Test
  def testPasting: Unit = {

    val sg1 = palette.getSG(1)

    val writer = MapWriter()
    val psg1 = writer.pasteSectorGroupAt(sg1, PointXYZ.ZERO)

    val sg11 = palette.getSG(11)
    Assert.assertFalse(sg11.allRedwallConnectors.isEmpty)
    Assert.assertEquals(1, sg11.allRedwallConnectors.head.getWallCount)

    val psg11 = writer.tryPasteConnectedTo(psg1, psg1.redwallConnectors.head, sg11, sg11.allRedwallConnectors.head)
    Assert.assertTrue(psg11.isDefined)

    // -----
  }

  @Test
  def testPaste2and12: Unit = {
    val writer = MapWriter()

    val psg2 = writer.pasteSectorGroupAt(palette.getSG(2), PointXYZ.ZERO)
    val sg12 = palette.getSG(12)
    writer.tryPasteConnectedTo(psg2, psg2.redwallConnectors.head, sg12, sg12.allRedwallConnectors.head)
  }
}
