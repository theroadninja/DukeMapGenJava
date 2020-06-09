package trn

import trn.{Map => DMap}
import trn.MapImplicits._

/**
  * Immutable interface for Map.  Replaces ImmutableMap.
  *
  * You can get this from a map with:
  *   import trn.MapImplicits._
  *   map.asView
  */
class MapView(map: DMap) {

  def getSector(sectorId: Int): Sector = map.getSector(sectorId)

  def allSectorIds: Seq[Int] = (0 until map.getSectorCount)

  def allWallViews: Seq[WallView] = map.allWallViews

  def allSprites: Seq[Sprite] = map.allSprites
}
