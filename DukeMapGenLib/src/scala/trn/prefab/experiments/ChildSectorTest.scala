package trn.prefab.experiments

import trn.prefab.{MapBuilder, MapWriter, PrefabPalette}
import trn.{DukeConstants, Main, MapLoader, MapUtil, PlayerStart, PointXY, PointXYZ, Sprite, Map => DMap}

import scala.collection.JavaConverters._

class TestBuilder(val outMap: DMap) extends MapBuilder {
  val writer = new MapWriter(this, this.sgBuilder)
}

@deprecated // see SectorGroupTests
object ChildSectorTest extends PrefabExperiment{

  override def Filename: String = "childtst.map"

  override def run(mapLoader: MapLoader): DMap = {
    val sourceMap = mapLoader.load(Filename)
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap, true);

    println(s"palette sector groups: ${palette.numberedSectorGroupIds().asScala}")

    val builder = new TestBuilder(DMap.createNew())

    builder.writer.pasteSectorGroup(palette.getSectorGroup(100), new PointXYZ(0, 0, 0))

    builder.writer.setAnyPlayerStart()
    builder.writer.clearMarkers()
    builder.outMap

  }
}
