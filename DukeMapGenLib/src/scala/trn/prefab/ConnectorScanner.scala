package trn.prefab

import trn.{AngleUtil, DukeConstants, IRayXY, ISpriteFilter, LineSegmentXY, MapView, PointXY, PointXYZ, Sector, Sprite, Wall, WallView, Map => DMap}
import trn.MapImplicits._
import org.apache.commons.lang3.tuple.{Pair => ApachePair}

import scala.collection.JavaConverters._
import scala.collection.mutable // this is the good one


class WallSection(val wallIds: Set[Int]) {
  var marker: Option[Sprite] = None
}

/**
  * Scala counterpart to ConnectorFactory
  */
object ConnectorScanner {
  val MultiSectWallLotag = 2

  // this really means "adjacent and connected"
  /**
    * @returns true if the walls are adjacent and connected but NOT if they are opposite walls forming a redwall
    */
  private[prefab] def adjacent(w0: WallView, w1: WallView): Boolean = {
    if(w0.p1 == w1.p2 && w0.p2 == w1.p1){
      false // they form a redwall, but havent been linked yet
    }else{
      w0.getWallId != w1.getWallId && w0.isRedwall == w1.isRedwall && !w0.isOtherSide(w1) && {
        // cannot let p1 == p1 because then we would detect walls in the other connect when two multi sectors
        // are already connected.
        w0.p1 == w1.p2 || w0.p2 == w1.p1
      }
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
    require(!walls.exists(_.lotag != MultiSectorConnector.WALL_LOTAG))
    require(walls.map(_.getWallId).toSet.size == walls.size, "duplicate wall ids detected")

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
        }.filterNot { n =>
          closedList.contains(n.getWallId)
        }

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
    *
    * See also:  MapUtil.isSpritePointedAtWall
    *
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

  /**
    * Choose a "pivot" point, which is an arbitrary point used to prevent the sorting algorithm from running endlessly
    * in a loop if the walls are in a cycle.
    *
    * @return (pivot, wall) where `pivot` is a point that halts the sort, and `wall` is the wall to start searching
    *         from.   In cases where its obvious there is no loop (i.e. pivot is not the first point of any wall) then
    *         the first wall in the given iterable is returned
    *
    * Note: in the case of wall loops,
    * this is not just the minx and miny like anchor, because that might not be a point on the wall loop.
    * To ensure this is deterministic, we take the min x first (ensuring x dominates y) and then take the y min.
    * The only way to defeat this is to have two points in exactly the same spot, which is not allowed for a valid
    * wall loop.
    *
    */
  def loopPivot(walls: Iterable[WallView]): (PointXY, WallView) = {
    val points: Iterable[PointXY] = walls.flatMap(_.points.asScala)
    val xmin = points.map(_.x).min
    val ymin = points.filter(_.x == xmin).map(_.y).min
    val rootPoint = new PointXY(xmin, ymin)
    val start = walls.find(_.p1 == rootPoint)
    if(start.isDefined){
      (rootPoint, start.get)
    }else{
      val w = walls.head
      (w.p1, w)
    }
  }

  /**
    * takes a set of unordered wall sections that make up a single wall and puts them in order.  The order
    * is based on the implicit order in a given wall, i.e. a wall with points (p0, p1) has a natural direction
    * p0 --> p1,  and p1 is the start of the next wall.  So the wall whose p0 does not match any other walls p1
    * is the first wall in the ordering.
    *
    * See also earler version(s):
    *   - ConnectorFactory.partitionWalls
    *   - MapUtil.sortWallSection
    *
    *
    * @param walls
    * @return
    */
  def sortContinuousWalls(walls: Iterable[WallView]): Seq[WallView] = {
    if(walls.isEmpty){
      Seq.empty
    }else{
      val firstPoint = walls.map(w => w.p1 -> w).toMap
      val secondPoint = walls.map(w => w.p2 -> w).toMap

      def forwardSearch(w: Option[WallView], rootPoint: PointXY): Seq[WallView] = {
        if(w.isEmpty){
          Seq.empty
        }else{
          val next = firstPoint.get(w.get.p2())
          if(next.isEmpty || next.get.p1 == rootPoint){
            Seq.empty
          }else{
            Seq(next.get) ++ forwardSearch(next, rootPoint)
          }

        }
      }
      def backwardSearch(w: Option[WallView], rootPoint: PointXY): Seq[WallView] = {
        if(w.isEmpty){
          Seq.empty
        }else{
          val prev = secondPoint.get(w.get.p1())
          if(prev.isEmpty){ // if its a cycle, we want backwardSearch to return nothing (because fowardSearch got it all)
            Seq.empty
          }else{
            Seq(prev.get) ++ backwardSearch(prev, rootPoint)
          }
        }

      }


      val (rootPoint, w) = loopPivot(walls) //val rootPoint = w.p1

      val fwalls = forwardSearch(Some(w), rootPoint)
      val results = if(fwalls.size + 1 == walls.size){
        // it was a loop; the foward search went all the way around
        Seq(w) ++ fwalls
      }else{
        backwardSearch(Some(w), rootPoint).reverse ++ Seq(w) ++ fwalls
      }

      if(results.size != walls.size){
        throw new IllegalArgumentException(s"walls not continuous ${results.size} != ${walls.size}")
      }
      results
    }
  }

  /**
    * TODO - there is duplicate logic in the MultiWallConnector constructor.
    *
    * Calculates the "anchor" point by finding the minimum x coord of all points, and the
    * minimum y coord of all points (they can come from differnt points, so the anchor might
    * not be on the wall).  This anchor point should be the same for two connectors of the same
    * shape which are facing each other.
    */
  def anchor(walls: Iterable[WallView]): PointXY = {
    if(walls.isEmpty){
      throw new IllegalArgumentException("walls cannot be empty")
    }
    val points = walls.flatMap(_.getLineSegment.toList.asScala)
    new PointXY(points.map(_.x).min, points.map(_.y).min)
  }

  // def matchSpritesToWallSections(sprites: Seq[Sprite], wallSections: Seq[WallSection])

  def findMultiSectorConnectors(map: MapView): java.util.List[Connector] = {
    // TODO - establish a max num of walls that can be used in this connector?  32? 512?


    // TODO - need to be able to limit the search by sector id...or wont be able to rescan PSGs
    val sectorIds = map.allSectorIds.toSet

    // TODO - only intersect sprites with walls that belong to that sector...
    // TODO - ensure that loops work

    val walls = map.allWallViews.filter(_.lotag == MultiSectWallLotag)
    if(walls.isEmpty){
      return new java.util.ArrayList[Connector]()
    }

    val pointToWall = pointToWallMap(walls)
    val wallsById: Map[Int, WallView] = walls.map(w => w.getWallId -> w).toMap

    val sprites = map.allSprites.filter(sprite => sectorIds.contains(sprite.getSectorId)).filter(isMultiSectorSprite)

    val wallSections = scanAllLines(walls, pointToWall).map { line =>
      new WallSection(line)
    }

    sprites.foreach { sprite =>

      val wallOpt = rayIntersect(sprite, map.sectorWallViewLoops(sprite.getSectorId).flatten)
      val wall = wallOpt.getOrElse(throw new MathIsHardException("marker sprite not pointed at a wall")) // should never happen
      SpriteLogicException.throwIf(
        wall.lotag() != MultiSectWallLotag,
        s"multi sector sprite pointed at wall that does not have lotag ${MultiSectWallLotag}",
      )

      val section = wallSections.find(_.wallIds.contains(wall.getWallId)).getOrElse(
        throw new SpriteLogicException("multi sector sprite encountered wierd error", sprite) // should never happen
      )

      if(section.marker.isDefined){
        val walls = s"walls=${section.wallIds}"
        val msg = s"2 multisect sprites pointed at same walls near ${section.marker.get.getLocation} sprite1.sector=${sprite.getSectorId} sprite2.sector=${section.marker.get.getSectorId} ${walls}"
        throw new SpriteLogicException(msg, sprite)
      }else{
        section.marker = Some(sprite)
      }
    }

    val wallIdToSectorId = map.newWallIdToSectorIdMap
    wallSections.filter(_.marker.isDefined).map { wallSection =>

      val walls = wallSection.wallIds.map(wallsById)
      val sortedWalls = sortContinuousWalls(walls)
      val wallIds = sortedWalls.map(_.getWallId)//.map(Integer.valueOf)
      val sectorIds = wallIds.map(wallIdToSectorId).toSeq

      MultiSectorConnector.create(
        wallSection.marker.get,
        sectorIds.map(Integer.valueOf).asJava,
        wallIds.map(Integer.valueOf).asJava,
        sortedWalls.asJava,
        anchor(sortedWalls), //.withZ(anchorZ),
        sortedWalls.head.p1,
        sortedWalls.last.p2,
        map
      )
    }.map(_.asInstanceOf[Connector]).asJava
  }

}
