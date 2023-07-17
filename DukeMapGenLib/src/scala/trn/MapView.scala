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
class MapView(map: DMap) extends WallContainer {

  override def getWall(id: Int): Wall = map.getWall(id)
  def getWallView(wallId: Int): WallView = map.getWallView(wallId)

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



  /** ************************************
    * TODO - functions to get rid of (or convert to scala types) later
    * *************************************/

  // def getAllSectorWallIds(sector: Sector): java.util.List[Integer] = map.getAllSectorWallIds(sector)
  // def getAllSectorWallIds(sectorId: Int): Seq[Int] = map.getAllSectorWallIds(sectorId).asScala.map(_.intValue)

  def getAllSectorWallIdsBySectorId(sectorId: Int): Seq[Int] = map.getAllSectorWallIds(sectorId).asScala.map(_.intValue)

    // need to be java integers because they can be null
  def findSprites(picnum: Integer, lotag: Integer, sectorId: Integer) = map.findSprites(picnum, lotag, sectorId)

}
