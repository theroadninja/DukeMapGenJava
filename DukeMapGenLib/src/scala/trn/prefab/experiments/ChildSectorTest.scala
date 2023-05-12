package trn.prefab.experiments

import trn.prefab.{MapWriter, DukeConfig, PrefabPalette, MapBuilder, GameConfig}
import trn.{HardcodedConfig, ScalaMapLoader, PointXYZ, Main, Map => DMap}

import scala.collection.JavaConverters._

class TestBuilder(
  val outMap: DMap,
  val gameCfg: GameConfig = DukeConfig.loadHardCodedVersion()
) extends MapBuilder {
  val writer = new MapWriter(this, this.sgBuilder)
}

@deprecated // see SectorGroupTests
object ChildSectorTest {

  def Filename: String = "childtst.map"

  def main(args: Array[String]): Unit = {
    val mapLoader = ScalaMapLoader(HardcodedConfig.DOSPATH)
    val map = run(mapLoader)
    Main.deployTest(map)
  }

  def run(mapLoader: ScalaMapLoader): DMap = {
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
