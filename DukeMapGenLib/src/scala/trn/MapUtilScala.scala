package trn

import trn.prefab.{ConnectorScanner, MathIsHardException, RedwallConnector, SectorGroup}

import scala.collection.JavaConverters._
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

}
