package trn.prefab

import org.junit.{Assert, Test}
import trn.{PointXYZ, Map => DMap}

class MapWriter2Tests {
  /*  BigRoom1:  connectorId(length)
   *
   * +-- 1(1024) -- 2(512) --+
   * |                       |
   * 8(512)                  3(1024)
   * |                       |
   * 7(1024)                 4(512)
   * |                       |
   * +-- 6(512) -- 5(1024) --+
   *
   * Sector Groups:
   * 2:   1 x 1024 connector (north)
   * 3:   2 x 1024 connector (north and south)
   * 4:   1 x 1024, 1 x 512 (north, south)
   * 5:   1 x 512 connector (north)
   * 6:   1 x 512, 1 x 1024 (north, south)
   * 7:   is like 2 but has wings that will overlap with any other sg on the same side
   * 8:   is like 3 but has wings that will overlap with any other sg on the same side
   */
  val BigRoom1 = 1
  val BigRoom2 = 2

  private def load(filename: String): DMap = TestUtils.loadTestMap(s"scala/trn.prefab/${filename}")

  lazy val palette: PrefabPalette = PrefabPalette.fromMap(load("UNIT2.MAP"), true);

  private def hasConnectors(result: Seq[Placement2], ids: Set[Int]): Unit = {
    Assert.assertEquals(ids.size, result.size)
    Assert.assertEquals(ids.size, result.map(_.extantConn.getConnectorId).toSet.intersect(ids.toSet).size)
  }

  def allConns(psg: PastedSectorGroup) = psg.redwallConnectors.map(c => (psg, c))

  @Test
  def testFindPlacementsRaw: Unit = {
    // val palette: PrefabPalette = PrefabPalette.fromMap(load("UNIT2.MAP"), true);

    val writer1 = MapWriter()
    val (room1, _) = writer1.sgBuilder.pasteSectorGroup2(palette.getSectorGroup(BigRoom1), PointXYZ.ZERO)

    Seq(2, 3, 4, 5, 6).foreach { doorId =>
      Assert.assertEquals(0, writer1.findPlacementsRaw(Seq.empty, palette.getSG(doorId)).size)
    }
    Assert.assertEquals(1, writer1.findPlacementsRaw(allConns(room1), palette.getSG(2)).size)
    Assert.assertEquals(0, writer1.findPlacementsRaw(allConns(room1), palette.getSG(2), Some(Seq.empty)).size)

    val door2Result1 = writer1.findPlacementsRaw(allConns(room1), palette.getSG(2))
    Assert.assertEquals(1, door2Result1.size)
    Assert.assertEquals(5, door2Result1.head.extantConn.getConnectorId)

    hasConnectors(writer1.findPlacementsRaw(allConns(room1), palette.getSG(3)), Set(1, 5))
    hasConnectors(writer1.findPlacementsRaw(allConns(room1), palette.getSG(4)), Set(2, 5))
    hasConnectors(writer1.findPlacementsRaw(allConns(room1), palette.getSG(5)), Set(6))
    hasConnectors(writer1.findPlacementsRaw(allConns(room1), palette.getSG(6)), Set(1, 6))

    // group 7 designed to overlap anything on the same side
    hasConnectors(writer1.findPlacementsRaw(allConns(room1), palette.getSG(7)), Set(5))
    hasConnectors(writer1.findPlacementsRaw(allConns(room1), palette.getSG(7).rotate180), Set(1))
    hasConnectors(writer1.findPlacementsRaw(allConns(room1), palette.getSG(8)), Set(6))
    hasConnectors(writer1.findPlacementsRaw(allConns(room1), palette.getSG(8).rotate180), Set(2))

    // PASTE GROUP 2
    Assert.assertFalse(room1.redwallConnectors.filter(_.getConnectorId == 5).head.isLinked(room1.map))
    writer1.pasteAndConnect(door2Result1.head)
    Assert.assertTrue(room1.redwallConnectors.filter(_.getConnectorId == 5).head.isLinked(room1.map))
    hasConnectors(writer1.findPlacementsRaw(allConns(room1), palette.getSG(7)), Set.empty)
    // because 2 is taking up the only south spot with a 1024 connector:
    hasConnectors(writer1.findPlacementsRaw(allConns(room1), palette.getSG(7), allowOverlap = true), Set.empty)
    hasConnectors(writer1.findPlacementsRaw(allConns(room1), palette.getSG(7).rotate180), Set(1))
    hasConnectors(writer1.findPlacementsRaw(allConns(room1), palette.getSG(8)), Set.empty)
    // test overlap
    hasConnectors(writer1.findPlacementsRaw(allConns(room1), palette.getSG(8), allowOverlap = true), Set(6))
    hasConnectors(writer1.findPlacementsRaw(allConns(room1), palette.getSG(8).rotate180), Set(2))

    // now that we already pasted 2 into the only slot available, there are no more placements for it
    val door2Result2 = writer1.findPlacementsRaw(allConns(room1), palette.getSG(2))
    Assert.assertEquals(0, door2Result2.size)

    // Rotation
    val door2Result3 = writer1.findPlacementsRaw(allConns(room1), palette.getSG(2).rotateCW)
    Assert.assertEquals(1, door2Result3.size)
    Assert.assertEquals(7, door2Result3.head.extantConn.getConnectorId)

    // TODO - test only using certain connectors
    // TODO -- both PSG, SG

  }

  @Test
  def testFindPlacementsRawSpecificConns: Unit = {

    val writer1 = MapWriter()
    val (room1, _) = writer1.sgBuilder.pasteSectorGroup2(palette.getSectorGroup(BigRoom1), PointXYZ.ZERO)

    val sg2 = palette.getSG(2)
    val sg3 = palette.getSG(3)

    hasConnectors(writer1.findPlacementsRaw(allConns(room1), sg2), Set(5))
    hasConnectors(writer1.findPlacementsRaw(allConns(room1), sg3), Set(1, 5))
    hasConnectors(writer1.findPlacementsRaw(allConns(room1), sg3, Some(Seq(MapWriter.south(sg3).get))), Set(1))
    hasConnectors(writer1.findPlacementsRaw(allConns(room1), sg3, Some(Seq(MapWriter.north(sg3).get))), Set(5))

    val extant = room1.redwallConnectors.filterNot(_.getConnectorId == 5).map(c => (room1, c))

    hasConnectors(writer1.findPlacementsRaw(extant, sg2), Set.empty)
    hasConnectors(writer1.findPlacementsRaw(extant, sg3), Set(1))
  }

  @Test
  def testFindPlacementsForSg: Unit = {
    val writer1 = MapWriter()
    val (room1, _) = writer1.sgBuilder.pasteSectorGroup2(palette.getSectorGroup(BigRoom1), PointXYZ.ZERO)

    hasConnectors(writer1.findPlacementsForSg(room1, palette.getSG(2), PasteOptions(allowRotate = false)), Set(5))
    hasConnectors(writer1.findPlacementsForSg(room1, palette.getSG(2)), Set(1, 3, 5, 7))
  }

  @Test
  def testTryPasteConnectedTo1: Unit = {
    val writer1 = MapWriter()
    val (room1, _) = writer1.sgBuilder.pasteSectorGroup2(palette.getSectorGroup(BigRoom1), PointXYZ.ZERO)

    Assert.assertTrue(writer1.tryPasteConnectedTo(room1, palette.getSG(2), PasteOptions(allowRotate = false)).isDefined)
    Assert.assertTrue(writer1.tryPasteConnectedTo(room1, palette.getSG(2), PasteOptions(allowRotate = false)).isEmpty)

    Assert.assertTrue(writer1.tryPasteConnectedTo(room1, palette.getSG(2), PasteOptions()).isDefined)
    Assert.assertTrue(writer1.tryPasteConnectedTo(room1, palette.getSG(2), PasteOptions()).isDefined)
    Assert.assertTrue(writer1.tryPasteConnectedTo(room1, palette.getSG(2), PasteOptions()).isDefined)
    Assert.assertTrue(writer1.tryPasteConnectedTo(room1, palette.getSG(2), PasteOptions()).isEmpty)
  }
}
