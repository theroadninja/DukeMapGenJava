package trn

import trn.{Map => DMap}
import trn.MapImplicits._
import scala.collection.JavaConverters._

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

  /** get all wall loops for a secotor, as wall views */
  def sectorWallViewLoops(sectorId: Int): Seq[Iterable[WallView]] = {
    map.getAllWallLoopsAsViews(sectorId).asScala.map(_.asScala)
  }

  // TODO - cache or lazy val.  Need to implement a dirty/changed flag in Map first?
  def newWallIdToSectorIdMap: scala.collection.Map[Int, Int] = {
    // there is probably a more efficient way to do this, but this is simpler for now
    (0 until map.getSectorCount).flatMap { sectorId =>
      map.getAllSectorWallIds(map.getSector(sectorId)).asScala.map(wallId => wallId.intValue -> sectorId)
    }.toMap
  }

}
