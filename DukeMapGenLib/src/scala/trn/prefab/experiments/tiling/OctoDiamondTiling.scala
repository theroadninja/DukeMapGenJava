package trn.prefab.experiments.tiling
import trn.{PointXY, RandomX}
import trn.{Map => DMap}
import trn.prefab.{MapWriter, SectorGroupBuilder, GameConfig, SectorGroup, SectorGroupProperties, Marker, BoundingBox, SectorGroupHints}
import trn.render.WallPrefab

/**
  * TODO also do octo squares!
  *
  *
  *  Axis-aligned Octogons with diamonds in their non-axis-aligned corners.
  *
  *    +     +------+         +
  *    .   /          \     /
  *  B . /             \  /
  *    +                +
  *    |                |
  *  A |   OCTAGON      |
  *    |                |
  *    +     C,R        +
  *      \            /  \
  *       \         /     \
  *        +------+  C+1,R +
  *       /         \     /
  *      /            \  /
  *    +               +
  *
  * A = octSide:  the length of an axis-aligned side of the octogon
  * B = diamondRadius:  the vertical distance from a side vertex to the top edge of the octogon, which is also
  *         the same distance as from a vertex on the diamond to it's center
  *
  *
  * @param octSide
  * @param diamondRadius
  */
class OctoDiamondTiling(octSide: Int, diamondRadius: Int) extends Tiling {

  /** height/width of the entire bounding box */
  val totalHeight = octSide + 2 * diamondRadius

  override def tileCoordinates(col: Int, row: Int): BoundingBox = {
    OctoDiamondTiling.tileType((col, row)) match {
      case OctoDiamondTiling.OctTile => {
        val octoTopLeft = new PointXY(col * totalHeight, row/2 * totalHeight)
        val octoBottomRight = new PointXY(octoTopLeft.x + totalHeight, octoTopLeft.y + totalHeight)
        //val octoBottomLeft = new PointXY((col + 1) * totalHeight, (row/2) * totalHeight)
        BoundingBox(Seq(octoTopLeft, octoBottomRight))
      }
      case OctoDiamondTiling.DiTile => {
        val octoTopLeft = new PointXY(col * totalHeight, (row-1)/2 * totalHeight)
        val octoBottomRight = new PointXY(octoTopLeft.x + totalHeight, octoTopLeft.y + totalHeight)
        val diamondTopLeft = new PointXY(octoBottomRight.x - diamondRadius, octoBottomRight.y - diamondRadius)
        val diamondBottomRight = new PointXY(octoBottomRight.x + diamondRadius, octoBottomRight.y + diamondRadius)
        BoundingBox(Seq(diamondTopLeft, diamondBottomRight))

      }

    }
  }

  def octoBoundingBox(topLeft: PointXY): BoundingBox = {
    val bottomLeft = new PointXY(topLeft.x + totalHeight, topLeft.y + totalHeight)
    BoundingBox(Seq(topLeft, bottomLeft))
  }

  def diBoundingBox(topLeft: PointXY): BoundingBox = {
    val bottomLeft = new PointXY(topLeft.x + 2 * diamondRadius, topLeft.y + 2 * diamondRadius)
    BoundingBox(Seq(topLeft, bottomLeft))
  }

  // override def calcEdges(coord: (Int, Int), neighboors: Seq[(Int, Int)]): Seq[Int] = OctoDiamondTiling.tileType(coord) match {
  //   case OctoDiamondTiling.OctTile => Seq.empty // TODO
  //   case OctoDiamondTiling.DiTile => Seq.empty // TODO
  // }
  override def edge(from: (Int, Int), to: (Int, Int)): Option[Int] = OctoDiamondTiling.tileType(from) match {
    case OctoDiamondTiling.OctTile => None // TODO
    case OctoDiamondTiling.DiTile => None // TODO
  }

  override def neighboors(coord: (Int, Int)): Seq[(Int, Int)] = ???  // TODO .map(Tiling.add(coord))

  def octoControlPoints(topLeft: PointXY): Seq[PointXY] = {
    val right = topLeft.x + octSide + 2 * diamondRadius
    val bottom = topLeft.y + octSide + 2 * diamondRadius
    Seq(
      new PointXY(topLeft.x + diamondRadius, topLeft.y), // top left
      new PointXY(topLeft.x + diamondRadius + octSide, topLeft.y), // top right
      new PointXY(right, topLeft.y + diamondRadius), // right top
      new PointXY(right, topLeft.y + diamondRadius + octSide), // right bottom
      new PointXY(topLeft.x + diamondRadius + octSide, bottom), // bottom right
      new PointXY(topLeft.x + diamondRadius, bottom), // bottom left
      new PointXY(topLeft.x, bottom - diamondRadius), // left bottom
      new PointXY(topLeft.x, topLeft.y + diamondRadius), // left top
    )
  }

  def diControlPoints(topLeft: PointXY): Seq[PointXY] = {
    val bottomRight = new PointXY(topLeft.x + 2 * diamondRadius, topLeft.y + 2 * diamondRadius)
    Seq(
      new PointXY(topLeft.x + diamondRadius, topLeft.y), // north
      new PointXY(bottomRight.x, topLeft.y + diamondRadius), // east
      new PointXY(topLeft.x + diamondRadius, bottomRight.y), // south
      new PointXY(topLeft.x, topLeft.y + diamondRadius), // west
    )
  }

}

object OctoDiamondTiling {
  val OctTile = 0
  val DiTile = 1

  def tileType(coord: (Int, Int)): Int = if(Math.abs(coord._2) % 2 == 0){
    OctTile
  } else {
    DiTile
  }

}

object OctoEdge {
  val E = 1
  val SE = 2
  val S = 3
  val SW = 4
  val W = 5
  val NW = 6
  val N = 7
  val NE = 8
}

object DiamondEdge {
  val NE = 1
  val SE = 2
  val SW = 3
  val NW = 4
}

class OctoDiOutline(tiling: OctoDiamondTiling) extends TileFactory {

  override def chooseTile(random: RandomX, coord: (Int, Int), tileType: Int, planNode: PlanNode, edges: Seq[Int]): String = "TODO"

  override def getTileMaker(gameCfg: GameConfig, name: String, tileType: Int): TileMaker = new OctoDiTileMaker(gameCfg, tiling)
}

class OctoDiTileMaker(gameCfg: GameConfig, tiling: OctoDiamondTiling) extends TileMaker {

  override def makeTile(gameCfg: GameConfig, name: String, tileType: Int, edges: Seq[Int]): SectorGroup = {
    tileType match {
      case OctoDiamondTiling.OctTile => {
        val bb = tiling.octoBoundingBox(PointXY.ZERO)

        // creating a new map, to create a new sector group
        val map = DMap.createNew()

        // val sectorId = ShapePrinter.renderBox(gameCfg, map, bb)
        val w = WallPrefab(gameCfg.tex(461))
        val walls = tiling.octoControlPoints(bb.topLeft).map(w.create)

        val sectorId = map.createSectorFromLoop(walls: _*)

        val marker = MapWriter.newMarkerSprite(sectorId, bb.center.withZ(map.getSector(sectorId).getFloorZ), lotag=Marker.Lotags.ANCHOR)
        map.addSprite(marker)
        val props = new SectorGroupProperties(None, false, None, Seq.empty, Seq.empty)
        SectorGroupBuilder.createSectorGroup(map, props, SectorGroupHints.Empty)

      }
      case OctoDiamondTiling.DiTile => {
        val bb = tiling.diBoundingBox(PointXY.ZERO)

        val map = DMap.createNew()
        val w = WallPrefab(gameCfg.tex(461))
        val walls = tiling.diControlPoints(bb.topLeft).map(w.create)
        val sectorId = map.createSectorFromLoop(walls: _*)
        ShapePrinter.addAnchor(map, sectorId, bb.center)
        SectorGroupBuilder.createSectorGroup(map, SectorGroupProperties.Default, SectorGroupHints.Empty)
      }

    }
  }
}
