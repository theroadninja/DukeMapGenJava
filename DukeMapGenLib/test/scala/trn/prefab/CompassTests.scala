package trn.prefab

import org.junit.{Assert, Test}
import trn.{JavaTestUtils, Map, PointXYZ}

// TODO more compass tests are in MapWriterTests
class CompassTests {

  // these connector ids are hardcoded in the map
  val EAST_CLOSE = 1
  val EAST_FAR = 2
  val SOUTH_CLOSE = 3
  val SOUTH_FAR = 4
  val WEST_CLOSE = 5
  val WEST_FAR = 6
  val NORTH_CLOSE = 7
  val NORTH_FAR = 8

  private def getPalette(): PrefabPalette = PrefabPalette.fromMap(TestUtils.loadTestMap(TestUtils.Compass))

  @Test
  def testConnector(): Unit = {
    val sg = getPalette().getSG(1)

    // TODO test both sg and psg

    Assert.assertTrue(Seq(1, 2).sameElements(sg.getCompassConnectors(Heading.E).map(_.getConnectorId).sorted))
    Assert.assertTrue(Seq(3, 4).sameElements(sg.getCompassConnectors(Heading.S).map(_.getConnectorId).sorted))
    Assert.assertTrue(Seq(5, 6).sameElements(sg.getCompassConnectors(Heading.W).map(_.getConnectorId).sorted))
    Assert.assertTrue(Seq(7, 8).sameElements(sg.getCompassConnectors(Heading.N).map(_.getConnectorId).sorted))

    Assert.assertTrue(Seq(1, 2).contains(CompassWriter.eastConnector(sg).getConnectorId))
    Assert.assertTrue(Seq(3, 4).contains(CompassWriter.southConnector(sg).getConnectorId))
    Assert.assertTrue(Seq(5, 6).contains(CompassWriter.westConnector(sg).getConnectorId))
    Assert.assertTrue(Seq(7, 8).contains(CompassWriter.northConnector(sg).getConnectorId))

    Assert.assertTrue(Seq(1, 2).contains(CompassWriter.east(sg).get.getConnectorId))

    val writer = MapWriter(DukeConfig.loadHardCodedVersion())
    val psg = writer.pasteSectorGroupAt(sg, PointXYZ.ZERO)

    Assert.assertTrue(Seq(1, 2).sameElements(psg.getCompassConnectors(Heading.E).map(_.getConnectorId).sorted))
    Assert.assertTrue(Seq(3, 4).sameElements(psg.getCompassConnectors(Heading.S).map(_.getConnectorId).sorted))
    Assert.assertTrue(Seq(5, 6).sameElements(psg.getCompassConnectors(Heading.W).map(_.getConnectorId).sorted))
    Assert.assertTrue(Seq(7, 8).sameElements(psg.getCompassConnectors(Heading.N).map(_.getConnectorId).sorted))

    Assert.assertTrue(Seq(1, 2).contains(CompassWriter.eastConnector(psg).getConnectorId))
    Assert.assertTrue(Seq(3, 4).contains(CompassWriter.southConnector(psg).getConnectorId))
    Assert.assertTrue(Seq(5, 6).contains(CompassWriter.westConnector(psg).getConnectorId))
    Assert.assertTrue(Seq(7, 8).contains(CompassWriter.northConnector(psg).getConnectorId))

    Assert.assertTrue(Seq(1, 2).contains(CompassWriter.east(psg).get.getConnectorId))
  }

  @Test
  def testFarthestConn(): Unit = {
    val sg = getPalette().getSG(1)

    Assert.assertEquals(EAST_FAR, CompassWriter.farthestConn(sg.getCompassConnectors(Heading.E), Heading.E).head.getConnectorId)
    Assert.assertEquals(SOUTH_FAR, CompassWriter.farthestConn(sg.getCompassConnectors(Heading.S), Heading.S).head.getConnectorId)
    Assert.assertEquals(WEST_FAR, CompassWriter.farthestConn(sg.getCompassConnectors(Heading.W), Heading.W).head.getConnectorId)
    Assert.assertEquals(NORTH_FAR, CompassWriter.farthestConn(sg.getCompassConnectors(Heading.N), Heading.N).head.getConnectorId)

  }

}
