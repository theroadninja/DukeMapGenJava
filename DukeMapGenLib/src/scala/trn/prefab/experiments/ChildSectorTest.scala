package trn.prefab.experiments

import trn.prefab.{MapBuilder, PrefabPalette}
import trn.{DukeConstants, Main, MapUtil, PlayerStart, PointXY, PointXYZ, Sprite, Map => DMap}

import scala.collection.JavaConverters._

class TestBuilder(val outMap: DMap) extends MapBuilder {
}

object ChildSectorTest {

  def run(sourceMap: DMap): DMap = {
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap, true);

    println(s"palette sector groups: ${palette.numberedSectorGroups.keySet().asScala}")

    val builder = new TestBuilder(DMap.createNew())

    builder.pasteSectorGroup(palette.getSectorGroup(100), new PointXYZ(0, 0, 0))

    builder.setAnyPlayerStart()
    builder.clearMarkers()
    builder.outMap
  }
}
