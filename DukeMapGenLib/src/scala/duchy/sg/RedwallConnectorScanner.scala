package duchy.sg

import duchy.sg.RedwallSection.MultiSection
import duchy.vector.{VectorMath, Line2D}
import trn.prefab.{ConnectorScanner, PrefabUtils, DukeConfig, ConnectorFactory2, RedwallConnector, ConnectorType, SpriteLogicException, Marker}
import trn.{PointXYZ, PointXY, MapUtil, WallView, MapView, Sprite}

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

  lazy val wallIds: Seq[Int] = sortedWalls.map(_.getWallId)

  /**
    * a seq of sectors ids meant to match the walls ids.  E.g. sectorIds(X) is the sectorId of the wall in wallIds(X)
    * This is a bit silly, since a RedwallSection is only for one sector and thus all walls have the same sectorId, but
    * it makes it simpler to code.
    */
  def sectorIds: Seq[Int] = wallIds.map(_ => sectorId)

  lazy val wallIdSet: Set[Int] = wallIds.toSet

  def overlaps(other: RedwallSection): Boolean = wallIdSet.intersect(other.wallIdSet).nonEmpty

  def isChild: Boolean = marker.getLotag == Marker.Lotags.MULTISECTOR_CHILD

  val isRedwall: Boolean = sortedWalls.head.isRedwall

  val lotag: Int = sortedWalls.head.lotag

  /**
    * @param otherSection
    * @return true if the last wall of this section lines up with the first wall of the next section.
    */
  def isBefore(otherSection: RedwallSection): Boolean = if(size < 1 || otherSection.size < 1){
    false
  }else{
    val tailWall = sortedWalls.last
    val headWall = otherSection.sortedWalls.head
    // the second check is to make sure we don't read joined connectors as a single connector
    tailWall.p2 == headWall.p1 && tailWall.otherSectorId() != otherSection.sectorId
  }
}

object RedwallSection {
  type MultiSection = Seq[RedwallSection]

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
  val MarkerLotags = Seq(Marker.Lotags.REDWALL_MARKER, Marker.Lotags.MULTISECTOR_CHILD)

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
    * @param wallsById map of wallId -> WallView.   Needs to contain every wall in the sector; its ok if it includes every
    *                  wall in the map
    * @param pointToWall map of point->walls  where `point` is one of the endpoints of the wall
    * @param startWall  the wall to start the search from
    */
  def crawlWalls(wallsById: Map[Int, WallView], pointToWall: Map[PointXY, Set[WallView]], wallIdToSector: Map[Int, Int], startWall: WallView): Set[WallView] = {
    require(RedwallConnectorLotags.contains(startWall.lotag))
    require(wallsById.contains(startWall.getWallId))

    val closedList = mutable.Set[Int]()
    val openList = mutable.ArrayBuffer[WallView]()
    openList.append(startWall)

    // a neightboor must share a point (and not be the opposing wall of a redwall) and also have the right lotag
    // and white vs redwall status
    def getNeighboor(wallView: WallView, p: PointXY): Option[WallView] = {
      val walls = pointToWall(p).filter { n =>
        val sector = wallIdToSector(wallView.getWallId)
        val otherSector = wallIdToSector(n.getWallId)
        adjacent(wallView, n) && n.lotag == wallView.lotag  && n.isRedwall == wallView.isRedwall && sector == otherSector
      }
      require(walls.size <= 1)  // TODO throw a nice SpriteLogic exception that contains the location of the marker sprite
      walls.headOption
    }

    while(openList.size > 0){
      val current = openList.remove(0)
      require(wallsById.contains(current.getWallId))
      closedList.add(current.getWallId)

      // 0 - 2 walls, the ones at either end of `current`
      val neighboors: Seq[WallView] = current.points.asScala.flatMap { point: PointXY =>
        getNeighboor(current, point)
      }.filterNot(n => closedList.contains(n.getWallId))

      openList ++= neighboors
    }

    closedList.map(wallsById).toSet
  }

  /**
    * Low level function that finds all pairs of (marker sprite, wall section).
    * @param map
    * @param sectorIdFilter
    * @return
    */
  def findAllRedwallSections(map: MapView, sectorIdFilter: Int => Boolean = allSectors): Seq[RedwallSection] = {
    val wallsById: Map[Int, WallView] = map.allWallViews.map(wv => wv.getWallId -> wv).toMap
    val pointsToWalls: Map[PointXY, Set[WallView]] = pointToWallMap(wallsById.values)
    val markers = map.allSprites.filter { s =>
      s.getTex == Marker.MARKER_SPRITE_TEX && MarkerLotags.contains(s.getLotag) && sectorIdFilter(s.getSectorId.toInt)
    }

    markers.map { marker =>
      val startWall = map.getWallView(findSpriteTargetWall(map, marker))
      if (!RedwallConnectorLotags.contains(startWall.lotag)) {
        throw new SpriteLogicException(s"redwall conn sprite is not pointing at a wall with a valid lotag", marker)
      }

      val sectorWallsById = map.getAllSectorWallIdsBySectorId(marker.getSectorId).map(wallId => wallId -> map.getWallView(wallId)).toMap
      val wallIdToSector = map.newWallIdToSectorIdMap.toMap
      val walls: Set[WallView] = crawlWalls(sectorWallsById, pointsToWalls, wallIdToSector, startWall)
      require(walls.nonEmpty)

      val sortedWalls = ConnectorScanner.sortContinuousWalls(walls)
      RedwallSection(marker, sortedWalls)
    }
  }

  /**
    * Match a "parent" section (contiguous walls with a marker sprite of lotag 20) with as many "child" sections as will
    * connect to it.  A child section is contiguous walls with a marker sprite of lotag 21.  Children can connect to
    * other children, as long as there is a parent somewhere.
    *
    * |-+--+--|----+--|-+-----|
    *    /\      /\      /\
    *    |       |       |
    *  (20)    (21)    (21)
    *
    * @param parent
    * @param children list of "child" wall segments that may or may not connect to the parent.
    * @return the sequence of walls sections that fit together
    */
  def matchParentToChildren(parent: RedwallSection, children: Seq[RedwallSection]): MultiSection = {
    var openList = children

    val results = mutable.ArrayBuffer[RedwallSection]()
    results.append(parent)

    var foundMatches: Boolean = true
    while(foundMatches) {
      val (suffixes, remaining) = openList.partition(section => results.last.isBefore(section))
      if(suffixes.size > 1) {
        throw new SpriteLogicException("more than one child Redwall Section is touching a parent Redwall Section", parent.marker)
      } else if(suffixes.size  == 1){
        results.append(suffixes.head)
      }

      val (prefixes, remaining2) = remaining.partition(section => section.isBefore(results.head))
      if(prefixes.size > 1){
        throw new SpriteLogicException("more than one child Redwall Section is touching a parent Redwall Section", parent.marker)
      } else if(prefixes.size == 1){
        results.prepend(prefixes.head)
      }

      foundMatches = !(suffixes.isEmpty && prefixes.isEmpty)
      openList = remaining2
    }

    results
  }

  /** get the XYZ position of the "anchor" for a multi-sector redwall connector */
  def getAnchor(walls: Seq[WallView], sectorIds: Seq[Int], map: MapView): PointXYZ = {
    val anchorXY = ConnectorScanner.anchor(walls)
    // The sector with the primary marker is not special, so use the lowest floor of alls sectors as the anchor
    val z = sectorIds.map(id => map.getSector(id).getFloorZ).max  // "max" means the "lowest" floor b/c z is inverted
    // val z = MultiSectorConnector.getAnchorZ(sectorIds.map(Integer.valueOf).asJava, map)
    anchorXY.withZ(z)
  }

  /**
    * For multi-sector connectors.  Returns the positions of all the wall points in the connector, but in coordinates
    * that are relative to the anchor.
    */
  def getRelativeConnPoints(walls: Seq[WallView], anchor: PointXY): Seq[PointXY] = if(walls.isEmpty) {
    Seq.empty
  } else {
    walls.map(_.p1.subtractedBy(anchor)) :+ walls.last.p2.subtractedBy(anchor)
  }

  /**
    * Makes sure the walls do not use door texture, which could screw up tag mapping (see updateUniqueTagsInPlace()
    * in GameConfig.scala).
    *
    * @param walls wallviews that are used in a redwall connector
    */
  def validateWalls(walls: TraversableOnce[WallView]): Unit = {
    // TODO this requires game-specific logic.  It would be better if the tag-mapping code was aware of which walls
    // were used in redwall conns
    walls.filter(w => DukeConfig.DoorTiles.contains(w.getTex)).foreach { w =>
      throw new SpriteLogicException(s"Redwall connector wall cannot have a door texture", w.p1())
    }
    // TODO make sure they don't have a door texture!
  }

  def findAllRedwallConns(map: MapView, sectorIdFilter: Int => Boolean = allSectors): Seq[RedwallConnector] = {

    val sections = findAllRedwallSections(map, sectorIdFilter)

    // detect overlap
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

    val multiSections: Seq[MultiSection] = parents.map { parent =>
      matchParentToChildren(parent, children)
    }

    // make sure none of the walls have a door tex
    multiSections.foreach { multiSection =>
      multiSection.foreach(section => validateWalls(section.sortedWalls))
    }

    multiSections.map { multiSection: MultiSection =>
      if(multiSection.size == 1){
        val section = multiSection.head
        RedwallConnector.create(
          section.marker,
          map.getSector(section.sectorId),
          section.sortedWalls.map(w => Integer.valueOf(w.getWallId)).asJava,
          section.sortedWalls.asJava,
          map,
        )
      }else{
        require(multiSection.size > 1)

        val parentMarker = multiSection.filter(!_.isChild).head.marker
        val wallIds: Seq[Int] = multiSection.flatMap(_.wallIds)
        val sectorIds: Seq[Int] = multiSection.flatMap(_.sectorIds)
        val wallViews = wallIds.map(wallId => map.getWallView(wallId))
        val anchor = getAnchor(wallViews, sectorIds, map)

        val connectorId = if(parentMarker.getHiTag() > 0){ parentMarker.getHiTag() }else{ -1 }

        val wallLotag = 1 // the "wall lotag" of a conn no longer has meaning

        val walls = wallViews.asJava
        val totalWallLength = WallView.totalLength(walls)
        val connectorType = ConnectorType.MULTI_SECTOR

        new RedwallConnector(
          connectorId,
          parentMarker.getSectorId,
          sectorIds.map(Integer.valueOf).asJava,
          totalWallLength,
          anchor,
          wallViews.head.p1,
          wallViews.last.p2,
          parentMarker.getLotag,
          connectorType,
          wallIds.map(Integer.valueOf).asJava,
          walls,
          wallLotag,
          getRelativeConnPoints(wallViews, anchor.asXY).asJava,
        )
      }

    }

  }

}
