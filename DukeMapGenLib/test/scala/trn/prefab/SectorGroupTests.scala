package trn.prefab

import org.junit.Test
import trn.PointXYZ
import trn.prefab.experiments.TestBuilder
import trn.{Map => DMap};
import scala.collection.JavaConverters._ // this is the good one

class SectorGroupTests {

  @Test
  def testChildGroups: Unit = {
    val map = TestUtils.loadTestMap("scala/trn.prefab/CHILDTST.MAP")

    val palette: PrefabPalette = PrefabPalette.fromMap(map, true);
    println(s"palette sector groups: ${palette.numberedSectorGroups.keySet().asScala}")
    val builder = new TestBuilder(DMap.createNew())
    builder.pasteSectorGroup(palette.getSectorGroup(100), new PointXYZ(0, 0, 0))
    builder.setAnyPlayerStart()
    builder.clearMarkers()
  }

}
