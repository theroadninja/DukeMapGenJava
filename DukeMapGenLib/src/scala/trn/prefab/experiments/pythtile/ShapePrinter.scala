package trn.prefab.experiments.pythtile

import trn.prefab.{BoundingBox, GameConfig}
import trn.render.WallPrefab
import trn.{Map => DMap}

object ShapePrinter {
  def renderBox(gameCfg: GameConfig, map: DMap, box: BoundingBox): Int = {
    val w = WallPrefab(gameCfg.tex(461))
    val walls = Seq(
      box.topLeft, box.topRight, box.bottomRight, box.bottomLeft
    ).map(w.create)

    val sectorId = map.createSectorFromLoop(walls: _*)
    sectorId
  }

}
