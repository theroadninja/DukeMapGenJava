package trn

import trn.prefab.{MathIsHardException, ConnectorScanner, Polygon, PrefabUtils, GameConfig, SectorGroup, RedwallConnector, SpriteLogicException, Marker}
import trn.{Map => DMap}

import scala.collection.JavaConverters._
import trn.MapImplicits._

import scala.collection.mutable

object MapUtilScala {

  def toScalaMap(map: java.util.Map[Integer, Integer]): scala.collection.immutable.Map[Int, Int] = {
    map.asScala.map{case (k, v) => k.intValue -> v.intValue}.toMap
  }

  /**
    * Returns the walls that make up the outer border of a sector group.   This algorithm is designed to exclude
    * holes that touch the edge of the sector group (see unit tests for examples).
    *
    * If you just want the outer loop of a single sector, see MapImplicits.getOuterWallLoop().
    *
    * @param walls  all walls in a sector group.  These walls must form valid build sectors for this to work
    * @return the walls that form the outer border of a sector group, in sequence.
    */
  def outerBorderDfs(walls: Iterable[WallView]): Seq[WallView] = {
    val wallViews = walls.filterNot(_.isRedwall)
    if(wallViews.size < 3) throw new IllegalArgumentException("must have at least 3 non-redwall walls")
    if(wallViews.size != wallViews.map(_.getWallId).toSet.size) {
      throw new IllegalArgumentException("wall ids must be distinct")
    }

    // make sure the walls dont cross
    walls.foreach { w0 =>
      walls.foreach { w1 =>
        if(w0 != w1 && !w0.contiguous(w1, false)){
          if(w0.getLineSegment.intersects(w1.getLineSegment)){
            throw new IllegalArgumentException(s"walls intersect: ${w0.getWallId} x ${w1.getWallId}")
          }
        }
      }
    }

    def getNormal(wall: WallView): LineXY = LineXY.fromPoints(wall.getLineSegment.midpoint(), wall.p2).rotatedCW()

    // this is not all of the outer walls, because the poly could be convex
    val someOuterWalls = wallViews.flatMap { wall =>
      val outer = ConnectorScanner.lineIntersectSorted(getNormal(wall), wallViews)
      if(outer.size < 2){
        throw new MathIsHardException(s"this should not be possible - check for invalid input: ${walls}")
      }else{
        Seq(outer.head, outer.last)
      }
    }.map(_.getWallId).toSet

    val wallsById = wallViews.toSeq.map(w => w.getWallId -> w).toMap
    val byP1 = wallViews.map(w=> w.p1-> w).groupBy{ case (k, _) => k}.mapValues(_.map(_._2).toSet)
    val startWallId = someOuterWalls.head

    // NOTE: this is n^2 because path is a seq.   Could be improved by using a set of points; not sure if worth it
    def dfs(startWall: WallView, path: Seq[WallView], byP1: scala.collection.immutable.Map[PointXY, Set[WallView]]): Seq[WallView] = {
      val nextWalls = byP1(path.last.p2)
      if(nextWalls.contains(startWall)){
        path
      }else{
        val results = nextWalls.filterNot(_.isRedwall).map { next =>
          if(path.exists(w => w != startWall && w.p1 == next.p2)){
            Seq.empty
          }else{
            dfs(startWall, path :+ next, byP1)
          }
        }.filterNot(_.isEmpty)
        val results2 = if(results.size < 2) {
          results
        }else {
          results.filter{wallSeq =>
            println(s"clockwise test for: ${wallSeq.map(_.getWallId)}")
            val b = Map.isClockwise(wallSeq.map(_.p1).asJava)
            println(s"result: ${b}")
            b
          }
        }
        // For debugging:
        // if(results2.count(_.nonEmpty) > 1){
        //   results2.foreach { walls => println(s"walls: ${walls.map(_.getWallId)}") }
        // }
        require(results2.size <= 1) // only one should have gone all the way around to the start
        results2.headOption.getOrElse(Seq.empty)
      }
    }
    val startWall = wallsById(startWallId)
    dfs(startWall, Seq(startWall), byP1)
  }

  /**
    * Return a loop full of walls that can be the other wall of a redwall.  This new loop has the same points, but
    * the individual walls go the opposite direction and their order is reversed.
    *
    *    +--1-->+                +<--4---+
    *    /\     |                |       /\
    *    4      2    =======>    1       3
    *    |      \/               \/      |
    *    +<--3--+                +---2-->+
    *
    * @param wallLoop
    * @return
    */
  def opposingRedwallLoop(wallLoop: Seq[WallView]): Seq[WallView] = wallLoop.reverse.map(_.reversed())


  /**
    * Copies an entire sector group _inside_ a single sector in the destination map, automatically creating the red
    * walls.
    *
    * The sector group will be positioned such that sourceAnchor and destAnchor are in the same place.
    *
    * @param sourceGroup the group to paste
    * @param sourceAnchor  anchoring sprite location inside the source group, matched with destAnchor
    * @param destMap  map to paste into
    * @param destSectorId id of sector in destMap to paste into
    * @param destAnchor destination anchoring sprite, matched with sourceAnchor
    * @param gameConfig config object that describes how to map unique ids
    */
  def copyGroupIntoSector(
    sourceGroup: SectorGroup,
    sourceAnchor: PointXYZ,
    destMap: DMap,
    destSectorId: Int,
    destAnchor: PointXYZ,
    gameConfig: GameConfig,
    changeUniqueTags: Boolean = true // TODO should this default to true?
  ): Unit = {

    // 1. calculate the border
    val sourceBorder = MapUtilScala.outerBorderDfs(sourceGroup.getAllWallViews)
    require(DMap.isClockwise(sourceBorder.map(_.p1).asJava))

    val delta = sourceAnchor.getTransformTo(destAnchor)
    val destBorder = MapUtilScala.opposingRedwallLoop(sourceBorder.map(_.translated(delta)))
    for(i <- 0 until destBorder.size){
      require(destBorder(i).p2 == destBorder((i+1) % destBorder.size).p1)
    }

    // 2. ensure the space is clear
    destMap.allWallViews.foreach { w0 =>
      destBorder.foreach { w1 =>
        if(w0.getLineSegment.intersects(w1.getLineSegment)){
          val msg = s"Cannot paste inner group, wall intersection: ${w0.getLineSegment} x ${w1.getLineSegment}"
          throw new SpriteLogicException(msg)
        }
      }
    }
    destMap.allSprites.find { sprite =>
      Polygon.containsExclusive2(destBorder, sprite.getLocation.asXY()) && sprite.getTex != Marker.MARKER_SPRITE_TEX
    }.foreach { sprite =>
      throw new SpriteLogicException(s"Cannot paste inner group, sprite in the way", sprite)
    }

    // 3. make sure all the points are in the destination sector
    val destSectorLoop = destMap.getOuterWallLoop(destSectorId)
    destBorder.flatMap(w => w.points().asScala).find{ p =>
      !Polygon.containsExclusive2(destSectorLoop, p)
    }.foreach { outOfBounds =>
      val msg = s"trying to paste group inside sector but wall point ${outOfBounds} does not fit inside dest sector"
      throw new SpriteLogicException(msg)
    }

    // 4. add the loop
    val outerWallIds = destMap.addLoopToSector(destSectorId, destBorder.map(_.getWall).asJava).asScala.map(_.toInt)
    val test1 = outerWallIds.map(destMap.getWallView(_))
    require(destBorder.flatMap(_.points().asScala).toSet == test1.flatMap(_.points().asScala).toSet)

    // 5. paste and link
    // Note: similar code in RedwallConnector.linkConnectors()
    val copyState = MapUtil.copySectorGroup(gameConfig, sourceGroup.map, destMap, 0, delta, changeUniqueTags)
    val innerWallIds = sourceBorder.map(_.getWallId).map(wallId => copyState.idmap.wall(wallId)).map(_.toInt)
    require(outerWallIds.size == innerWallIds.size)

    val outerWalls = outerWallIds.map(destMap.getWallView(_))
    for(i <- 0 until outerWalls.size){
      require(outerWalls(i).p2 == outerWalls((i+1) % outerWalls.size).p1)
    }
    // println(s"outer loop: ${outerWallIds.map(dest.map.getWallView(_).getLineSegment)}")
    // println(s"inner loop: ${innerWallIds.map(dest.map.getWallView(_).getLineSegment)}")

    val wallIdToSectorId = new MapView(destMap).newWallIdToSectorIdMap
    outerWallIds.reverse.zip(innerWallIds).foreach { case (outerId, innerId) =>
      //println(destMap.getWallView(outerId).getLineSegment)
      //println(destMap.getWallView(innerId).getLineSegment)
      require(destMap.getWallView(outerId).isBackToBack(destMap.getWallView(innerId)))
      val newSectorId = wallIdToSectorId(innerId)
      destMap.linkRedWalls(destSectorId, outerId, newSectorId, innerId)
    }

  }

}
