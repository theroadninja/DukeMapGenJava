package trn.prefab.experiments.tiling
import trn.{PointXY, RandomX, Wall, Map => DMap}
import trn.prefab.{BoundingBox, GameConfig, MapWriter, PrefabUtils, SectorGroup, SectorGroupBuilder, SectorGroupHints, SectorGroupProperties}
import trn.render.WallPrefab


/**
  *
  *
  *  ..+...................+..........
  *      .               .   .
  *        .   c,r     .       .
  *          .       .           .
  *            .   .    c+1,r      .
  *  ............+...................+
  *            .   .               .
  *          .       .  c+1,r+1  .
  *        .           .       .
  *      .    c,r+1      .   .
  *  ..+...................+.........
  *      .               .   .
  *        .  c,r+2    .       .
  *          .       .           .
  *            .   .               .
  *  ............+...................+
  *
  *
  *
  */
object TriangleEdge {
  val E = 0
  val S = 1
  val W = 2
  val N = 3

  def edge(from: (Int, Int), to: (Int, Int)): Option[Int] = {
    val dx = to._1 - from._1
    val dy = to._2 - from._2
    if(TriangleTiling.pointingDown(from)) {
      // triangle pointing down
      (dx, dy) match {
        case (0, -1) => Some(N)
        case (1, 0) => Some(E)
        case (-1, 0) => Some(W)
        case _ => None
      }
    }else{
      (dx, dy) match {
        case (1, 0) => Some(E)
        case (-1, 0) => Some(W)
        case (0, 1) => Some(S)
        case _ => None
      }
    }
  }

}

/**
  * These are not true equilateral triangles (and not heronian triangles either).  They are designed to look like
  * equilateral triangles, while fitting nicely on the Build grid.  The height to width ratio is 3:4
  *
  *              +
  *            . . .
  *          .   .   .
  *        .     .     .
  *      .       .       .
  *    .         .h=3      .
  *  +.......................+
  *              w=4
  *
  * w is width, h is height (not hypotenuse)
  *
  * Basically, these triangles are just a little too wide.
  *
  *
  */
class TriangleTiling(val width: Int) extends Tiling {
  require(width > 0 && width % 4 == 0)
  private val x = width / 4
  require((x & (x - 1)) == 0)  // make sure width/4 is a power of 2
  val height = x * 3

  override def tileCoordinates(col: Int, row: Int): BoundingBox = {
    val y = row * height
    val x = if (Math.abs(col) % 2 == 0) {
      col / 2 * width
    }else{
      (col - 1) / 2 * width + width/2
    }
    BoundingBox(x, y, x + width, y + height)
  }

  override def edge(from: (Int, Int), to: (Int, Int)): Option[Int] = TriangleEdge.edge(from, to)

  override def neighboors(coord: (Int, Int)): Seq[(Int, Int)] = if(TriangleTiling.pointingDown(coord)){
    Seq((0, -1), (1, 0), (-1, 0))
  }else{
    Seq((1, 0), (-1, 0), (0, 1))
  }.map(Tiling.add(coord))
}

object TriangleTiling {
  /**
    * 0, 0 starts pointing down
    * if (col + row) is even, the triangle is pointing down
    * @param coord
    * @return
    */
  def pointingDown(coord: (Int, Int)): Boolean = {
    val (col, row) = coord
    (Math.abs(col) + Math.abs(row)) % 2 == 0
  }
}

class TriangleOutline(tiling: TriangleTiling) extends TileFactory {
  override def chooseTile(random: RandomX, coord: (Int, Int), tileType: Int, edges: Seq[Int]): String = {
    if(TriangleTiling.pointingDown(coord)){
      "DOWN"
    }else{
      "UP"
    }
  }

  override def getTileMaker(gameCfg: GameConfig, name: String, tileType: Int): TileMaker = {
    new TriangleOutlineTileMaker(gameCfg, tiling)
  }
}

class TriangleOutlineTileMaker(gameCfg: GameConfig, tiling: TriangleTiling) extends TileMaker {
  override def makeTile(gameCfg: GameConfig, name: String, tileType: Int, edges: Seq[Int]): SectorGroup = {

    val bb = BoundingBox(0, 0, tiling.width, tiling.height)

    // creating a new map, to create a new sector group
    val map = DMap.createNew()

    val w = WallPrefab(gameCfg.tex(461))

    val walls: Seq[Wall] = if(name == "DOWN"){
      Seq(
        bb.topLeft, bb.topRight, new PointXY(bb.center.x, bb.yMax)
      ).map(w.create)
    }else{
      Seq(
        bb.bottomLeft, new PointXY(bb.center.x, bb.yMin), bb.bottomRight
      ).map(w.create)
    }
    val sectorId = map.createSectorFromLoop(walls: _*)

    ShapePrinter.addAnchor(map, sectorId, bb.center)
    SectorGroupBuilder.createSectorGroup(map, SectorGroupProperties.Default, SectorGroupHints.Empty)
  }
}