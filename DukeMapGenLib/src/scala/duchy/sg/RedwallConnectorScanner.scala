package duchy.sg

import duchy.vector.{VectorMath, Line2D}
import trn.prefab.{ConnectorScanner, PrefabUtils, ConnectorFactory2, RedwallConnector, MultiSectorConnector, SpriteLogicException}
import trn.{PointXY, MapUtil, WallView, MapView, Sprite}

import scala.collection.JavaConverters._
import scala.collection.immutable.Map
import scala.collection.mutable

/**
  * A sequence of walls in the same sector that are intended to be all or part of a redwall connector.
  * @param marker
  * @param sortedWalls
  */
case class RedwallSection(marker: Sprite, sortedWalls: Seq[WallView]) {
  def sectorId: Int = marker.getSectorId

  def size: Int = sortedWalls.size

  lazy val wallIds: Set[Int] = sortedWalls.map(_.getWallId).toSet

  def overlaps(other: RedwallSection): Boolean = wallIds.intersect(other.wallIds).nonEmpty

  def isChild: Boolean = marker.getLotag == PrefabUtils.MarkerSpriteLoTags.MULTISECTOR_CHILD

  val isRedwall: Boolean = sortedWalls.head.isRedwall

  val lotag: Int = sortedWalls.head.lotag
}

/**
  * Meant to replace ALL of the redwall connector scanning I wrote before.
  *  - SimpleConnectorScanner.scala
  *  - ConnectorScanner.scala
  *  - ConnectorFactory, ConnectorFactory2.java
  *
  * Idea:
  * - start w/ the primary marker-20s and follow contiguous walls in the same sector (dont cross between white
  * and red walls)
  * - detect sprite pointed at the same wall group; sprites with no wall group
  * - add an isLoop()?   (loops will need some kind of arbitrary start and end, or need to fix all of the code to
  *   work without those two things)
  * - parse multisector children
  * - ensure no wall overlap
  * - match children to parents
  */
object RedwallConnectorScanner {
  val MarkerLotags = Seq(PrefabUtils.MarkerSpriteLoTags.SIMPLE_CONNECTOR, PrefabUtils.MarkerSpriteLoTags.MULTISECTOR_CHILD)

  val RedwallConnectorLotags: Seq[Int] = Seq(RedwallConnector.WALL_LOTAG_1, RedwallConnector.WALL_LOTAG_2, RedwallConnector.WALL_LOTAG_3)

  // TODO duplicated from SimpleConnectorScanner.allSectors
  private def allSectors(sectorId: Int): Boolean = true

  def intersectSpriteWall(sprite: Sprite, wallView: WallView): Option[PointXY] = {
    // TODO add implicits for asSprite(), etc
    VectorMath.intersection(Line2D.spriteRay(sprite), Line2D.wallSegment(wallView)).filterNot { point =>
      // ignore if it intersects the end of the segment, because it should intersect the beginning of the next segment
      point == wallView.p2
    }
  }

  /**
    * Find the wall of the sector that intersects the sprites ray.
    *
    * @param map
    * @param sprite
    * @returns the id of the nearest wall that the sprite is pointing at
    */
  def findSpriteTargetWall(map: MapView, sprite: Sprite): Int = {
    val intersections = map.getAllSectorWallIdsBySectorId(sprite.getSectorId).flatMap { wallId =>
      intersectSpriteWall(sprite, map.getWallView(wallId)).map { point => (wallId, point)}
    }
    if(intersections.isEmpty){
      throw new RuntimeException("sprite ray does not intersect any walls in the sector")
    }
    val (wallId, _) = intersections.minBy{
      case (_, intersection) => sprite.getLocation.asXY.manhattanDistanceTo(intersection)
    }
    wallId
  }

  def adjacent(w0: WallView, w1: WallView): Boolean = ConnectorScanner.adjacent(w0, w1)

  def pointToWallMap(walls: Iterable[WallView]): Map[PointXY, Set[WallView]] = ConnectorScanner.pointToWallMap(walls)

  /**
    * Finds all contiguous walls with same lotag, same color (red vs white) in the same sector
    *
    * TODO copying this (sort of) from ConnectorScanner.scanAllLines()
    *
    * @param wallsById map of wallId -> WallView.   Needs to contain very wall in the sector; its ok if it includes every
    *                  wall in the map
    * @param pointToWall map of point->walls  where `point` is one of the endpoints of the wall
    * @param startWall  the wall to start the search from
    */
  def crawlWalls(wallsById: Map[Int, WallView], pointToWall: Map[PointXY, Set[WallView]], startWall: WallView): Set[WallView] = {
    require(RedwallConnectorLotags.contains(startWall.lotag))
    require(wallsById.contains(startWall.getWallId))

    val closedList = mutable.Set[Int]()
    val openList = mutable.ArrayBuffer[WallView]()
    openList.append(startWall)

    // a neightboor must share a point (and not be the opposing wall of a redwall) and also have the right lotag
    // and white vs redwall status
    def getNeighboor(wallView: WallView, p: PointXY): Option[WallView] = {
      val walls = pointToWall(p).filter { n =>
        adjacent(wallView, n) && n.lotag == wallView.lotag  && n.isRedwall == wallView.isRedwall
      }
      require(walls.size <= 1)  // TODO throw a nice SpriteLogic exception that contains the location of the marker sprite
      walls.headOption
    }

    while(openList.size > 0){
      val current = openList.remove(0)
      closedList.add(current.getWallId)

      // 0 - 2 walls, the ones at either end of `current`
      val neighboors: Seq[WallView] = current.points.asScala.flatMap { point: PointXY =>
        getNeighboor(current, point)
      }.filterNot(n => closedList.contains(n.getWallId))

      openList ++= neighboors
    }

    closedList.map(wallsById).toSet
  }

  def findAllRedwallSections(map: MapView, sectorIdFilter: Int => Boolean = allSectors): Seq[RedwallSection] = {
    val wallsById: Map[Int, WallView] = map.allWallViews.map(wv => wv.getWallId -> wv).toMap
    val pointsToWalls: Map[PointXY, Set[WallView]] = pointToWallMap(wallsById.values)
    val markers = map.allSprites.filter { s =>
      s.getTex == PrefabUtils.MARKER_SPRITE_TEX && MarkerLotags.contains(s.getLotag) && sectorIdFilter(s.getSectorId.toInt)
    }

    markers.map { marker =>
      val startWall = map.getWallView(findSpriteTargetWall(map, marker))
      if (!RedwallConnectorLotags.contains(startWall.lotag)) {
        throw new SpriteLogicException(s"redwall conn sprite is not pointing at a wall with a valid lotag", marker)
      }

      val sectorWallsById = map.getAllSectorWallIdsBySectorId(marker.getSectorId).map(wallId => wallId -> map.getWallView(wallId)).toMap
      val walls: Set[WallView] = crawlWalls(sectorWallsById, pointsToWalls, startWall)
      require(walls.nonEmpty)

      val sortedWalls = ConnectorScanner.sortContinuousWalls(walls)
      RedwallSection(marker, sortedWalls)
    }
  }

  def findAllRedwallConns(map: MapView, sectorIdFilter: Int => Boolean = allSectors): Seq[RedwallConnector] = {

    val sections = findAllRedwallSections(map, sectorIdFilter)

    sections.foreach { sectionA =>
      sections.foreach { sectionB =>
        if(sectionA.marker != sectionB.marker){
          if(sectionA.overlaps(sectionB)){
            val locations = Seq(sectionA.marker, sectionB.marker).map(_.getLocationXY).asJava
            throw new SpriteLogicException("more than one redwall conn sprite is pointing at the same wall section", locations)
          }
        }
      }
    }

    // assemble children and parents

    val (children, parents) = sections.partition(_.isChild)
    if(children.nonEmpty) {
      throw new RuntimeException("TODO multi-sector connectors not supported yet")
    }
    parents.map { section =>

      // TODO  so the RedwallConnector constructor will throw here if its a loop
      RedwallConnector.create(
        section.marker,
        map.getSector(section.sectorId),
        section.sortedWalls.map(w => Integer.valueOf(w.getWallId)).asJava,
        section.sortedWalls.asJava,
        map,
      )
    }

    // // TODO for multisector, something like this happens at the end
    // val wallIdToSectorId = map.newWallIdToSectorIdMap
    // sections.foreach { section =>


    //   MultiSectorConnector.create(
    //     section.marker,
    //     sectorIds.map(Integer.valueOf).asJava,
    //     wallIds.map(Integer.valueOf).asJava,
    //     sortedWalls.asJava,
    //     anchor(sortedWalls), //.withZ(anchorZ),
    //     sortedWalls.head.p1,
    //     sortedWalls.last.p2,
    //     map
    //   )
    // }
  }

}
