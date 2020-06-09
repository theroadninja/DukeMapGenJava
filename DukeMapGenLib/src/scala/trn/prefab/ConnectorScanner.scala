package trn.prefab

import trn.{AngleUtil, DukeConstants, IRayXY, ISpriteFilter, LineSegmentXY, MapView, PointXY, PointXYZ, Sector, Sprite, Wall, WallView, Map => DMap}
import trn.MapImplicits._
import org.apache.commons.lang3.tuple.{Pair => ApachePair}

import scala.collection.JavaConverters._
import scala.collection.mutable // this is the good one


class WallSection(wallIds: Set[Int]) {
  var marker: Option[Sprite] = None
}

/**
  * Scala counterpart to ConnectorFactory
  */
object ConnectorScanner {
  val MultiSectWallLotag = 2

  // this really means "adjacent and connected"
  private[prefab] def adjacent(w0: WallView, w1: WallView): Boolean = {
    w0.getWallId != w1.getWallId && w0.isRedwall == w1.isRedwall && !w0.isOtherSide(w1) && {
      // cannot let p1 == p1 because then we would detect walls in the other connect when two multi sectors
      // are already connected.
      w0.p1 == w1.p2 || w0.p2 == w1.p1
    }
  }

  /**
    * creates a map of PointXY -> Seq[WallView]  where each key is pointing to all of the walls containing that
    * point.
    */
  def pointToWallMap(walls: Iterable[WallView]): Map[PointXY, Set[WallView]] = {
    val x = walls.flatMap(wall => wall.getLineSegment.toList.asScala.map(p => p -> wall)).toSeq
    val x2 = x.groupBy{ case (k, _) => k}
    x2.mapValues(_.map(_._2).toSet)
  }

  // finds a bunch of walls that are all connected to each other
  def scanLine(walls: Seq[WallView], pointToWall: Map[PointXY, Set[WallView]] ): (Set[Int], Seq[WallView]) = {
    require(walls.size > 0)
    require(!walls.exists(_.lotag != PrefabUtils.MarkerSpriteLoTags.MULTI_SECTOR))

    val closedList = mutable.Set[Int]()
    val openList = mutable.ArrayBuffer[WallView]()
    openList.append(walls(0))
    //println(s"starting with ${openList.head.getWallId}")

    while(openList.size > 0){
      val current = openList.remove(0)
      closedList.add(current.getWallId)

      val points: Seq[PointXY] = current.getLineSegment.toList.asScala
      val neighboors = points.flatMap { p: PointXY =>

        val neighboorsForPoint = pointToWall.get(p).getOrElse(Set.empty[WallView]).filter { n: WallView =>
          adjacent(current, n)
        }.filterNot(n => closedList.contains(n.getWallId))

        // for each vertex, there should only be one neighboring wall with lotag 2, same red/white, which is not the "other" redwall
        SpriteLogicException.throwIf(neighboorsForPoint.size > 1, "multisector connector problem (TODO better err msg)")
        neighboorsForPoint
      }
      openList ++= neighboors
    }

    (closedList.toSet, walls.filterNot(w => closedList.contains(w.getWallId)))
  }

  def scanAllLines(walls: Seq[WallView], pointToWall: Map[PointXY, Set[WallView]]): Seq[Set[Int]] = {
    require(walls.size > 0)

    val (results, remaining) = scanLine(walls, pointToWall)
    if(remaining.isEmpty){
      Seq(results)
    }else{
      Seq(results) ++ scanAllLines(remaining, pointToWall)
    }
  }

  private[prefab] def isMultiSectorSprite(s: Sprite): Boolean = {
    s.getTex == PrefabUtils.MARKER_SPRITE_TEX && s.getLotag == PrefabUtils.MarkerSpriteLoTags.MULTI_SECTOR
  }

  /**
    * Using the sprites position and angle to form a ray, intersect it with all walls and return the closest one.
    * @param sprite
    * @return the closest wall that intersected
    */
  def rayIntersect(sprite: IRayXY, walls: Seq[WallView]): Option[WallView] = {

    def rayInt(s: IRayXY, wall: LineSegmentXY): Option[Double] = {
      Option(PointXY.rayIntersectForTU(s, wall, false)).map(_.getLeft.doubleValue)
    }

    val intersectedWalls = walls.map(w => (w, rayInt(sprite, w.getLineSegment))).collect {
      case (wall, dist) if dist.isDefined => (wall, dist.get)
    }
    if(intersectedWalls.isEmpty){
      None
    }else{
      val (closest, _) = intersectedWalls.minBy(_._2)
      Some(closest)
    }
  }

  def findMultiSectorConnectors(map: MapView): java.util.List[MultiSectorConnector] = {
    // TODO - use a "map view" here, since we dont need to modify the map

    // TODO - establish a max num of walls that can be used in this connector?  32? 512?


    // TODO - need to be able to limit the search by sector id...or wont be able to rescan PSGs
    val sectorIds = map.allSectorIds.toSet

    // TODO - only intersect sprites with walls that belong to that sector...
    // TODO - ensure that loops work
    val results = new java.util.ArrayList[MultiSectorConnector]()

    val walls = map.allWallViews.filter(_.lotag == MultiSectWallLotag)
    if(walls.isEmpty){
      return results
    }

    val pointToWall = pointToWallMap(walls)
    val wallsById: Map[Int, WallView] = walls.map(w => w.getWallId -> w).toMap

    val sprites = map.allSprites.filter(sprite => sectorIds.contains(sprite.getSectorId)).filter(isMultiSectorSprite)

    val wallSections = scanAllLines(walls, pointToWall).map { line =>
      new WallSection(line)
    }

    // see  this in connector factory:
    //private static boolean matches(Sprite marker, List<Integer> walls, Map map){
    //MapUtil.isSpritePointedAtWall

    // for each sprite
    //   find the FIRST wall the sprite's ray intersects
    //   check if the wall has lotag 22
    //   get the wall id
    //   scan through the wall sections for one that contains the wall id
    //   store the sprite in the wall section (if one is already there, throw error)


    // TODO - enforce the thing about must have more than one sector, unless water
    ???

    // every marker should point to a different set of walls with lotag 2
  }

}
