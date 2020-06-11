package trn.prefab

import org.junit.{Assert, Test}
import trn.PointXYZ
import trn.prefab.experiments.TestBuilder
import trn.{Map => DMap}

import scala.collection.JavaConverters._ // this is the good one

class SectorGroupTests {

  // TODO - this is a dupe of the one in TestUtils
  private def load(filename: String): DMap = TestUtils.loadTestMap(s"scala/trn.prefab/${filename}")

  private def loadPalette: PrefabPalette = PrefabPalette.fromMap(load("UNIT.MAP"), true)
  private def eastConn(psg: PastedSectorGroup): SimpleConnector = {
    psg.findFirstConnector(SimpleConnector.EastConnector).asInstanceOf[SimpleConnector]
  }
  private def westConn(psg: PastedSectorGroup): SimpleConnector = {
    psg.findFirstConnector(SimpleConnector.WestConnector).asInstanceOf[SimpleConnector]
  }


  @Test
  def testPlayerStart: Unit = {
    val palette: PrefabPalette = PrefabPalette.fromMap(load("UNIT.MAP"), true);
    // UNIT.MAP notes:
    // - Sector Group 1 only has an id
    // - Sector Group 2 has an id and a player start marker

    Assert.assertFalse(palette.getSectorGroup(1).hasPlayerStart)
    Assert.assertTrue(palette.getSectorGroup(2).hasPlayerStart)

    val builder = new UnitTestBuilder(DMap.createNew())
    builder.placeAnywhere(palette.getSectorGroup(1))
    try{
      builder.writer.setAnyPlayerStart()
      Assert.fail("exception should have been thrown")
    }catch{
      case _: Exception => {}
    }
    builder.placeAnywhere(palette.getSectorGroup(2))
    builder.writer.setAnyPlayerStart()
  }

  @Test
  def testAnchor: Unit = {
    val palette = loadPalette
    val builder = new UnitTestBuilder(DMap.createNew())

    palette.getSectorGroup(3).getAnchor
    palette.getSectorGroup(4).getAnchor

    // the anchor sprites are placed such that they will be 2048 apart when the sectors are linked
    val left = builder.writer.pasteSectorGroupAt(palette.getSectorGroup(3), new PointXYZ(0, 0, 0))
    val right = builder.writer.pasteSectorGroupAt(palette.getSectorGroup(4), new PointXYZ(2048, 0, 0))
    //builder.joinWalls(eastConn(left), westConn(right))
    SimpleConnector.linkConnectors(eastConn(left), westConn(right), builder.outMap)

    // more of a sanity check, to test this test
    val left2 = builder.writer.pasteSectorGroupAt(palette.getSectorGroup(3), new PointXYZ(0, 1024 * 10, 0))
    val right2 = builder.writer.pasteSectorGroupAt(palette.getSectorGroup(4), new PointXYZ(0, 1024 * 10, 0))
    try {
      //builder.joinWalls(eastConn(left2), westConn(right2))
      SimpleConnector.linkConnectors(eastConn(left2), westConn(right2), builder.outMap)
      Assert.fail("expected an exception")
    } catch {
      case _: Exception => {}
    }
  }

  @Test
  def testChildGroups: Unit = {
    //val map = TestUtils.loadTestMap("scala/trn.prefab/CHILDTST.MAP")
    val map = TestUtils.load(TestUtils.ChildTest)

    val palette: PrefabPalette = PrefabPalette.fromMap(map, true);
    println(s"palette sector groups: ${palette.numberedSectorGroupIds().asScala}")
    val builder = new TestBuilder(DMap.createNew())
    builder.writer.pasteSectorGroup(palette.getSectorGroup(100), new PointXYZ(0, 0, 0))
    builder.writer.setAnyPlayerStart()
    builder.writer.clearMarkers()
  }



}
