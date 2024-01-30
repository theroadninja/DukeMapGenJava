package trn.prefab.experiments.tiling

import trn.{Map => DMap}
import trn.{PointXY, RandomX, Wall}
import trn.prefab.{BoundingBox, GameConfig, SectorGroup, SectorGroupBuilder, SectorGroupHints, SectorGroupProperties}
import trn.render.WallPrefab


/**
  * +               +               +
  *   . c-1, r-1  .   .  c, r-1   .
  *     .       .       .       .
  *       .   .           .   .
  *         +               +
  *         .               .
  *         .               .
  * c-1, r  .     c,r       .  c+1, r
  *         .               .
  *         .               .
  *         +               +
  *       .   .           .   .
  *     .       .       .       .
  *   .           .   .           .
  * +   c-1, r+1    +    c, r+1     +
  */
object HexEdge {
  val E = 1
  val SE = 2
  val SW = 3
  val W = 4
  val NW = 5
  val NE = 6

  def edge(from: (Int, Int), to: (Int, Int)): Option[Int] = {
    val dx = to._1 - from._1
    val dy = to._2 - from._2
    (dx, dy) match {
      case (1, 0) => Some(E)
      case (0, 1) => Some(SE)
      case (-1, 1) => Some(SW)
      case (-1, 0) => Some(W)
      case (-1, -1) => Some(NW)
      case (0, -1) => Some(NE)
      case _ => None
    }
  }
}


/**
  * These hexes are made of the same faux-equilateral triangles as in TriangleTiling, except they are rotated 90 degrees,
  * meaning the long side is vertical, which makes these hexes taller vertically.
  *
  *
  *                           _
  *             +             |
  *           .   .           |
  *         .       .         |
  *       .           .       |
  *     +               +     |
  *     .               .     |
  *     .   3x          .     |
  * 4x  . . . . +       .     h = 8x
  *     .       .       .     |
  *     .       .       .     |
  *     +       .4x     +     |
  *       .     .     .       |
  *         .   .   .         |
  *           . . .           |
  *             +             _
  *
  *     |--- w = 6x ----|
  *
  * If `x` is some scaling factor, then:
  * - the hex fits into a bounding box of width=6x, height=8x
  * - the distance from a vertical side to the center is 3x
  * - the distance from the top or bottom point to the center is 4x
  */
class HexTiling(val width: Int) extends Tiling {
  require(width > 0 && width % 6 == 0)
  private val f = width / 6
  require((f & (f - 1)) == 0)  // make sure width/6 is a power of 2

  val height = 8 * f

  val verticalSide = 4 * f

  override def tileCoordinates(col: Int, row: Int): BoundingBox = {
    val x = if(row % 2 == 0) { col * width } else { col * width + (width/2)}
    val y = row * (height - (height - verticalSide)/2) // TODO this is wrong

    BoundingBox(x, y, x + width, y + height)
  }

  override def edge(from: (Int, Int), to: (Int, Int)): Option[Int] = HexEdge.edge(from, to)

  override def neighboors(coord: (Int, Int)): Seq[(Int, Int)] = Seq(
    (1, 0), (0, 1), (-1, 1), (-1, 0), (-1, -1), (0, -1),
  ).map(Tiling.add(coord))
}

class HexOutline(tiling: HexTiling) extends TileFactory {
  override def chooseTile(random: RandomX, coord: (Int, Int), tileType: Int, planNode: PlanNode, edges: Seq[Int]): String = ""

  override def getTileMaker(gameCfg: GameConfig, name: String, tileType: Int): TileMaker = new HexOutlineTileMaker(tiling)
}

class HexOutlineTileMaker(tiling: HexTiling) extends TileMaker {
  override def makeTile(gameCfg: GameConfig, name: String, tileType: Int, edges: Seq[Int]): SectorGroup = {

    val bb = BoundingBox(0, 0, tiling.width, tiling.height)

    // creating a new map, to create a new sector group
    val map = DMap.createNew()
    val w = WallPrefab(gameCfg.tex(461))
    val sideUpperY = bb.center.y - tiling.verticalSide / 2
    val sideLowerY = bb.center.y + tiling.verticalSide / 2
    val walls: Seq[Wall] = Seq(
      new PointXY(bb.center.x, bb.yMin), // N
      new PointXY(bb.xMax, sideUpperY),
      new PointXY(bb.xMax, sideLowerY),
      new PointXY(bb.center.x, bb.yMax), // S
      new PointXY(bb.xMin, sideLowerY),
      new PointXY(bb.xMin, sideUpperY),
    ).map(w.create)
    val sectorId = map.createSectorFromLoop(walls: _*)

    ShapePrinter.addAnchor(map, sectorId, bb.center)
    SectorGroupBuilder.createSectorGroup(map, SectorGroupProperties.Default, SectorGroupHints.Empty)
  }
}
