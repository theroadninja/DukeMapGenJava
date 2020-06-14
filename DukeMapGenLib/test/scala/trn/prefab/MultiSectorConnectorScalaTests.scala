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

  @Test
  def testPaste3and13: Unit = {
    val writer = MapWriter()
    val sg3 = palette.getSG(3)
    val psg3 = writer.pasteSectorGroupAt(sg3, PointXYZ.ZERO)
    Assert.assertEquals(2, psg3.redwallConnectors.head.getWallCount)
    val sg13 = palette.getSG(13)

    val conn3 = sg3.allRedwallConnectors.head
    println(s"wallAnchor1=${conn3.wallAnchor1} anchor=${conn3.anchor} relativePoints=${conn3.relativePoints}")
    println(sg13.allRedwallConnectors.head.relativePoints)
    Assert.assertEquals(2, sg13.allRedwallConnectors.head.getWallCount)
    val p = writer.tryPasteConnectedTo(psg3, psg3.redwallConnectors.head, sg13, sg13.allRedwallConnectors.head)
    Assert.assertTrue(p.isDefined)
  }

  @Test
  def test4and4: Unit = {
    val writer = MapWriter()
    val psg4 = writer.pasteSectorGroupAt(palette.getSG(4), PointXYZ.ZERO)
    val eastConn = psg4.redwallConnectors.find(_.getConnectorId == 123).get

    val sg4 = palette.getSG(4)
    val westConn = sg4.allRedwallConnectors.find(_.getConnectorId == -1).get
    val p = writer.tryPasteConnectedTo(psg4, eastConn, sg4, westConn)
    Assert.assertTrue(p.isDefined)
  }

  @Test
  def test6and16: Unit = {
    val writer = MapWriter()
    val psg16 = writer.pasteSectorGroupAt(palette.getSG(16), PointXYZ.ZERO)
    val conn16 = psg16.redwallConnectors.head
    Assert.assertEquals(6, conn16.getWallCount)
    println(conn16.relativePoints)
    Assert.assertFalse(conn16.isLinked(writer.outMap))

    val sg6 = palette.getSG(6)
    val conn6 = sg6.allRedwallConnectors.head
    Assert.assertEquals(6, conn6.getWallCount)
    println(conn6.relativePoints)
    Assert.assertFalse(conn6.isLinked(sg6.getMap))

    Assert.assertTrue(conn16.isMatch(conn6))
    Assert.assertTrue(writer.canPlaceAndConnect(conn16, conn6, sg6, false))

    // TODO - dont yet support pasing inside loops because the overlap detection doesnt support that
    // options:
    // 1. improve polygon overlap detection to supprt holes
    // 2. set a `loop` flag on connectors and, when true, always disable overlap detection

    //Assert.assertTrue(writer.canPlaceAndConnect(conn16, conn6, sg6, true))
    //val existing = Seq((psg16, conn16))
    //val allPlacements = existing.flatMap {
    //  case (psg, existingConn) => Seq(conn6).map{ newConn =>
    //    Placement2(psg, existingConn, sg6, newConn)
    //  }
    //}
    //Assert.assertEquals(1, allPlacements.size)
    //val placements = writer.findPlacementsRaw(existing, sg6, Some(Seq(conn6)))
    //Assert.assertTrue(placements.size > 0)
    //val p = writer.tryPasteConnectedTo(psg16, conn16, sg6, conn6)
    //Assert.assertTrue(p.isDefined)
  }
}
