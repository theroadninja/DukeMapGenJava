package trn.prefab.experiments.pythtile

import trn.math.SnapAngle
import trn.prefab.PrefabUtils.MarkerHiTags
import trn.prefab._
import trn.prefab.experiments.ExpUtil
import trn.render.WallPrefab
import trn.{BuildConstants, HardcodedConfig, MapLoader, PointXY, RandomX, Sprite, Map => DMap}

import scala.collection.mutable.ArrayBuffer

object PythTileType {
  val BigTile = 0
  val SmallTile = 1

  def tileType(col: Int, row: Int): Int = if(Math.abs(row) % 2 == 0) { BigTile }else{ SmallTile }

  def tileType(coords: (Int, Int)): Int = tileType(coords._1, coords._2)
}

// origin is sort of the top left, if it was rotated to be square
// tiles on the right will be above the origin
class PythagoreanTiling(origin: PointXY, val bigW: Int, val smallW: Int) extends Tiling {
  require(bigW > smallW)

  override def tileCoordinates(col: Int, row: Int): BoundingBox = {
    // TODO:  this is locked to big tiles being 2x the little ones
    if(row % 2 == 0){
      // big tile
      val x = bigW * col + smallW * (row/2)
      val y = bigW * (row/2) - smallW * col
      BoundingBox(x, y, x + bigW, y + bigW).translate(origin)
    } else {
      // little tile
      val x = bigW * col + smallW * ((row-1)/2)
      val y = bigW * ((row-1)/2) - smallW * col + bigW
      BoundingBox(x, y, x + smallW, y + smallW).translate(origin)
    }
  }

  override def calcEdges(coord: (Int, Int), neighboors: Seq[(Int, Int)]): Seq[Int] = PythagoreanTiling.calcEdges(coord, neighboors)
}

object PythagoreanTiling {
  def apply(origin: PointXY, bigW: Int, smallW: Int) = new PythagoreanTiling(origin, bigW, smallW)

  /**
    * Given a coordinate of an occupied space, and a list of neighboors, calculate all edges that touch one of the
    * given neighboors.
    * @param coord  the coordinate of the tile to calculate edges for
    * @param neighboors coordinates of other tiles, which may or may not be adjacent
    * @return list of any edges that are shared between the tile a `coord` and any neighbooring tiles in the list
    */
  def calcEdges(coord: (Int, Int), neighboors: Seq[(Int, Int)]): Seq[Int] = {
    if(PythTileType.tileType(coord) == PythTileType.BigTile){
      neighboors.flatMap(n => BigTileEdge.edge(coord, n))
    } else {
      neighboors.flatMap(n => SmallTileEdge.edge(coord, n))
    }
  }
}


/**
  *             |    c, r-1     |
  *             |               |
  *   c-1, r-1  +-------+-------+--
  *             |       |
  *             | (c,r) |
  *  ---+-------+-------+   c, r+1
  *     |               |
  *     |   c-1, r+1    |
  *
  */
object SmallTileEdge {
  val E = 1
  val S = 2
  val W = 3
  val N = 4
  val all = Seq(E, S, W, N)

  def edge(from: (Int, Int), to: (Int, Int)): Option[Int] = {
    require(Math.abs(from._2) % 2 == 1, s"coordinate ${from} is not a small tile")
    val dx = to._1 - from._1
    val dy = to._2 - from._2
    (dx, dy) match {
      case (0, 1) => Some(E)
      case (-1, 1) => Some(S)
      case (-1, -1) => Some(W)
      case (0, -1) => Some(N)
      case _ => None
    }
  }

  def rotateCW(edgeId: Int): Int = {
    require(edgeId > 0 && edgeId < 5)
    if(edgeId == N){
      E
    }else{
      edgeId + 1
    }
  }

  def rotationToMatch(from: Int, to: Int): SnapAngle = {
    require(all.contains(from) && all.contains(to))
    // see also SnapAngle.rotateUntil()
    lazy val r90 = rotateCW(from)
    lazy val r180 = rotateCW(r90)
    if (from == to) {
      SnapAngle(0)
    } else if (r90 == to) {
      SnapAngle(1)
    } else if (r180 == to) {
      SnapAngle(2)
    } else {
      SnapAngle(3)
    }
  }

  def opposite(a: Int, b: Int): Boolean = Math.abs(a - b) == 2
}

/**
  *                   |
  *                   +-----+---------------
  *         (c, r-2)  |+1,-1|
  *             NB    | NS  |
  *  ----+------------+-----+
  *    WS|                  |
  * c,r-1|                  |
  *  ----+                  |EB (c+1, r)
  *      |                  |
  *    WB|       (c,r)      +-----+
  * c-1,r|                  |ES   |
  *      |                  |+1,+1|
  *      +-----+------------+-----+
  *      | SS  |     SB           |
  *      |c,r+1|     (c, r+2)     |
  * -----+-----+                  |
  *
  */
object BigTileEdge {
  // 1-based so that these IDs can match the connectors
  val ES = 1 // east, small
  val EB = 2 // east, big
  val SS = 3 // south, small
  val SB = 4
  val WS = 5
  val WB = 6
  val NS = 7
  val NB = 8
  val all = Seq(ES, EB, SS, SB, WS, WB, NS, NB)

  /**
    * If `to` is next to `from`, it returns which edge on `from` that `to` is touching
    * @param from (col, row)
    * @param to (col, row)
    * @return
    */
  def edge(from: (Int, Int), to: (Int, Int)): Option[Int] = {
    require(from._2 % 2 == 0)
    val dx = to._1 - from._1
    val dy = to._2 - from._2
    (dx, dy) match {
      case (1, 0) => Some(EB)
      case (1, 1) => Some(ES)
      case (0, 2) => Some(SB)
      case (0, 1) => Some(SS)
      case (-1, 0) => Some(WB)
      case (0, -1) => Some(WS)
      case (0, -2) => Some(NB)
      case (1, -1) => Some(NS)
      case _ => None
    }
  }
}

trait TileMaker {
  def makeTile(name: String, tileType: Int, edges: Seq[Int]): SectorGroup
}

trait TileFactory {
  // def smallWidth: Int
  // def bigWidth: Int
  def chooseTile(random: RandomX, coord: (Int, Int), tileType: Int, edges: Seq[Int]): String
  def getTileMaker(gameCfg: GameConfig, name: String, tileType: Int): TileMaker
  // def palette: PrefabPalette
}

object TileMaker {
  def attachHallway(gameConfig: GameConfig, sg: SectorGroup, attachments: Map[Int, SectorGroup], attachId: Int): SectorGroup = {
    val hallwaySg = attachments(attachId)
    val connId = attachId
    sg.withGroupAttached(gameConfig, sg.getRedwallConnectorsById(connId).head, hallwaySg, hallwaySg.getRedwallConnectorsById(connId).head)
  }
}


class Outline(tiling: PythagoreanTiling) extends TileFactory {
  def smallWidth: Int = tiling.smallW
  def bigWidth: Int = tiling.bigW

  override def chooseTile(random: RandomX, coord: (Int, Int), tileType: Int, edges: Seq[Int]): String = ""

  override def getTileMaker(gameCfg: GameConfig, name: String, tileType: Int): TileMaker = new PythOutlineTileMaker(gameCfg, tiling)
}

class PythOutlineTileMaker(gameCfg: GameConfig, tiling: PythagoreanTiling) extends TileMaker {
  override def makeTile(name: String, tileType: Int, edges: Seq[Int]): SectorGroup = {
    val bb = tileType match {
      case PythTileType.BigTile => BoundingBox(0, 0, tiling.bigW, tiling.bigW)
      case PythTileType.SmallTile => BoundingBox(0, 0, tiling.smallW, tiling.smallW)
    }

    // TODO this code is duplicated in RectOutlineTileMaker
    // creating a new map, to create a new sector group
    val map = DMap.createNew()
    val sectorId = ShapePrinter.renderBox(gameCfg, map, bb)
    val marker = MapWriter.newMarkerSprite(sectorId, bb.center.withZ(map.getSector(sectorId).getFloorZ), lotag=PrefabUtils.MarkerSpriteLoTags.ANCHOR)
    map.addSprite(marker)
    val props = new SectorGroupProperties(None, false, None, Seq.empty)
    SectorGroupBuilder.createSectorGroup(map, props, SectorGroupHints.Empty)
  }

}


object PythagoreanTiling1 {

  def printTiles(gameCfg: GameConfig, writer: MapWriter, tiling: PythagoreanTiling): Unit = {
    for(col <- 0 until 8){
      for (row <- 0 until 16){
        ShapePrinter.renderBox(gameCfg, writer.outMap, tiling.tileCoordinates(col, row))
      }
    }
  }

}
