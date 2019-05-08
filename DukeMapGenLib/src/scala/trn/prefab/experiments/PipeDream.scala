package trn.prefab.experiments

import trn.prefab._
import trn.{MapLoader, PointXY, PointXYZ, Wall, Map => DMap}
import scala.collection.JavaConverters._

import scala.collection.mutable.ListBuffer

class PipeBuilder(val outMap: DMap, palette: PrefabPalette) extends MapBuilder {

}


object PipeDream {
  val FILENAME = "pipe.map"

  def run(mapLoader: MapLoader): DMap = {
    val sourceMap = mapLoader.load(FILENAME)
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap);
    val builder = new PipeBuilder(DMap.createNew(), palette)

    val stays = palette.getStaySectorGroups.asScala
    stays.foreach{ sg =>
      builder.pasteSectorGroup(sg, PointXYZ.ZERO) // no translate == leave where it is
    }

    // TODO - builder needs to auto-accumulate all PSGs

    //builder.pasteSectorGroup(palette.getSectorGroup(2), PointXYZ.ZERO)

    println(s"Sector count: ${builder.outMap.getSectorCount}")
    builder.setAnyPlayerStart()
    builder.clearMarkers()
    builder.outMap
  }
}
