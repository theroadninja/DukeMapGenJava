package trn.prefab

import trn.{DukeConstants, ISpriteFilter, PointXY, PointXYZ, Sector, Sprite, Wall, WallView, Map => DMap}
import trn.MapImplicits._

import scala.collection.JavaConverters._
import scala.collection.mutable // this is the good one
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

  def pointToWallMap(walls: Iterable[WallView]): Map[PointXY, Set[WallView]] = {
    val x = walls.flatMap(wall => wall.getLineSegment.toList.asScala.map(p => p -> wall)).toSeq
    val x2 = x.groupBy{ case (k, _) => k}
    x2.mapValues(_.map(_._2).toSet)
  }

  def findMultiSectorConnectors(map: DMap): java.util.List[MultiSectorConnector] = {

    // TODO - ensure that loops work
    val results = new java.util.ArrayList[MultiSectorConnector]()

    val walls = map.allWallViews.filter(_.lotag == MultiSectWallLotag)
    if(walls.isEmpty){
      return results
    }


    // val pointToWall0 = mutable.Map[PointXY, mutable.Set[WallView]]()
    // walls.foreach { wall =>
    //   wall.getLineSegment.toList.asScala.foreach{p =>
    //     pointToWall0.getOrElseUpdate(p, mutable.Set()).add(wall)
    //   }
    // }
    // val pointToWall = pointToWall0.map{ case (k, v) => k -> v.toSet }.toMap

    val pointToWall = pointToWallMap(walls)

    val wallsById: Map[Int, WallView] = walls.map(w => w.getWallId -> w).toMap


    def scanAllLines(walls: Seq[WallView]): Seq[Set[Int]] = {
      require(walls.size > 0)

      val (results, remaining) = scanLine(walls, pointToWall)
      if(remaining.isEmpty){
        Seq(results)
      }else{
        Seq(results) ++ scanAllLines(remaining)
      }
    }

    ???

    // every marker should point to a different set of walls with lotag 2
  }

}
