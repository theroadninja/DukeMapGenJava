package trn.prefab.experiments.tiling

import trn.prefab.{BoundingBox, MapWriter, GameConfig, Marker}
import trn.render.WallPrefab
import trn.{Sprite, PointXY, Map => DMap}

object ShapePrinter {
  def renderBox(gameCfg: GameConfig, map: DMap, box: BoundingBox): Int = {
    val w = WallPrefab(gameCfg.tex(461))
    val walls = Seq(
      box.topLeft, box.topRight, box.bottomRight, box.bottomLeft
    ).map(w.create)

    val sectorId = map.createSectorFromLoop(walls: _*)
    sectorId
  }

  def addAnchor(map: DMap, sectorId: Int, loc: PointXY): Sprite = {
    val marker = MapWriter.newMarkerSprite(sectorId, loc.withZ(map.getSector(sectorId).getFloorZ), lotag=Marker.Lotags.ANCHOR)
    map.addSprite(marker)
    marker
  }

}
