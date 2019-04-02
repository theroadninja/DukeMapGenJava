package trn.prefab.experiments

import trn.prefab._
import trn.{DukeConstants, Main, MapUtil, PointXY, PointXYZ, Sprite, Map => DMap}
import trn.MapImplicits._
import trn.duke.PaletteList


class Hyper2MapBuilder(val outMap: DMap) extends MapBuilder {
}
object Hypercube2 {


  // note:  6 large squares ( 1042 ) seem like a good size.

  def run(sourceMap: DMap): DMap = {
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap);
    val builder = new Hyper2MapBuilder(DMap.createNew())

    val basicRoom = palette.getSectorGroup(100)
    builder.pasteSectorGroupAt(basicRoom, new PointXYZ(0, 0, 0))


    builder.setAnyPlayerStart()
    builder.clearMarkers()
    builder.outMap
  }


}
