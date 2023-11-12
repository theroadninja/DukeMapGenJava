package trn.prefab.experiments.dijkdrop

import trn.prefab.{Enemy, SectorGroup, SpritePrefab, Item, Marker}

/**
  * Creating this class to hold candidates for more generic functions
  */
object Utils {

  /**
    * replace ALL markers with the specified lotag with a sequence of sprites.  The sprites
    * must be pre-shuffled before calling, because the order they will be matched is neither
    * random nor deterministic.
    *
    * @param sg the sector group to modify
    * @param markerLotag the lotag of the markers to be replaced
    * @param shuffledSprites a shuffled sequence of sprites to insert
    * @return
    */
  def withRandomSprites(sg: SectorGroup, markerHitag: Int, markerLotag: Int, shuffledSprites: Seq[SpritePrefab]): SectorGroup = {
    val slots: Int = sg.allSprites.filter(s => Marker.isMarker(s, markerLotag) && s.getHiTag == markerHitag).size
    (0 until slots).map(i => shuffledSprites(i % shuffledSprites.size)).foldLeft(sg) { case (sg2, item) => sg2.withMarkerReplaced(markerHitag, markerLotag, item) }
  }

  def withRandomEnemies(sg: SectorGroup, enemies: Seq[SpritePrefab]) = {
    withRandomSprites(sg, 0, Marker.Lotags.ENEMY, enemies)
  }

}
