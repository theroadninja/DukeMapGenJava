package trn.prefab

import trn.{PointXY, Sprite, Wall, Map => DMap}
import trn.FuncImplicits._

import scala.collection.mutable
import scala.collection.JavaConverters._ // this is the good one

object FullWall {
  def apply(wallId: Int, map: DMap): FullWall = {
    val wall = map.getWall(wallId)
    FullWall(wallId, wall, map.getWall(wall.getPoint2Id).getLocation)
  }
}

// NOTE: p1 and p2 are ordered
case class FullWall(wallId: Int, wall: Wall, point2: PointXY) {
  def p1: PointXY = wall.getLocation
  def p2: PointXY = point2
  def intersects(s: Sprite): Boolean = s.intersectsSegment(p1, p2)
}

case class MultiSectorCandidate(connectorId: Int, sectorId: Int, sprite: Sprite, wall: FullWall) {
  def p1: PointXY = wall.p1
  def p2: PointXY = wall.p2
}

/**
  * Trying to move some of the logic to a scala factory.
  */
object ConnectorFactory2 {

  /** find the wall in the sprite's sector that the sprite intersects */
  def intersectedWall(s: Sprite, map: DMap): FullWall = {
    val sector = map.getSector(s.getSectorId)
    val walls = map.getAllSectorWallIds(sector).asScala.map(FullWall(_, map)).filter(_.intersects(s))
    require(walls.size != 0, "sprite did not intersect any walls; did you pass the wrong map?")
    if(walls.size > 1){
      // TODO - this might not be an error.  Could be caused by a concave sector...
      throw new RuntimeException("Not Implemented Yet")
    }else{
      walls.head
    }
  }

  /**
    * We need to split the seq up into sets of walls that form a line.
    *
    * In graph theory terms, the walls are directed edges, and we are sort of finding the connectd components of a graph
    * except that each component must be a line and not a tree and all of the edges must be going in the same direction.
    *
    * @param walls
    */
  def partitionMultiSectorWalls(walls: Seq[MultiSectorCandidate]): Unit = {

    // none of these (directional) walls can share the same p1, or the same p2
    // TODO - switch to the one in func utils
    def assertNoDuplicates(points: Seq[PointXY]): Unit = {
      points.duplicates.headOption.foreach(p => throw new SpriteLogicException("multi-sector connector invalid", p))
    }
    assertNoDuplicates(walls.map(_.p1))
    assertNoDuplicates(walls.map(_.p2))

    //  [p1] ----> [p2]
    case class Node(p: PointXY) {
      val incomingLinks = mutable.ListBuffer[MultiSectorCandidate]() // p2
      val outGoingsLinks = mutable.ListBuffer[MultiSectorCandidate]() // p1
    }
    val nodes = (walls.map(_.p1) ++ walls.map(_.p2)).distinct.map(Node(_)).map(n => n.p -> n).toMap
    walls.foreach { w =>
      nodes(w.p1).incomingLinks.append(w)
      nodes(w.p2).incomingLinks.append(w)
    }

    def component(openlist: Map[PointXY, Node]) = {
      // identify one segment and return it
      // return the openlist with that segments nodes removed
    }

    // TODO - should openlist and closedlist be points?


    // val p1map = walls.map(w => w.p1 -> w).toMap
    // val p2map = walls.map(w => w.p2 -> w).toMap
    //
    //

    // def partitionSegment(walls: Seq[MultiSectorCandidate]): (Seq[MultiSectorCandidate], Seq[MultiSectorCandidate]) = {
    //   if(walls.size < 1){
    //     throw new IllegalArgumentException("walls is empty")
    //   }
    //   val openlist = mutable.ListBuffer()
    //   walls.copyToBuffer(openlist)

    //   val segment = mutable.ListBuffer[MultiSectorCandidate]()
    //   segment.append(openlist.remove(0))

    //   while(p1map.contains(segment.last.p2)){
    //     val w = p1map(segment.last.p2)
    //     segment.append(p1map(segment.last.p2))
    //     openlist.fil
    //   }
    //   ???
    // }



    ???
  }

  def findConnectors(map: DMap): Seq[Connector] = {

    // TODO:  get distinct tuples of (connectorId, sectorId, spriteId, (wallId, Wall))
    // (if more than one sprite points at the same wall, error

    val multiSectorSprites: Seq[Sprite] = map.findSprites(PrefabUtils.MARKER_SPRITE).asScala.filter(_.getLotag == ConnectorType.MULTI_SECTOR).toSeq


    val y = multiSectorSprites.map { sprite =>
      MultiSectorCandidate(sprite.getHiTag, sprite.getSectorId, sprite, intersectedWall(sprite, map))
    }.histogram(_.connectorId)

    // TODO - still need to partition on non-contiguous walls
    // NOTE:  can't use ConnectorFactory because that is only Intra-Sector ...



    ???
  }

  // overload for java to call
  def findConnectorsJ(map: DMap): java.util.List[Connector] = findConnectors(map).asJava

}
