package trn.prefab.experiments.tiling
import trn.RandomX
import trn.{Map => DMap}
import trn.prefab.{BoundingBox, GameConfig, MapWriter, PrefabUtils, SectorGroup, SectorGroupBuilder, SectorGroupHints, SectorGroupProperties}


/**
  *
  *                  |
  *      c-1, r-1    |      c, r-1
  *  ------+---------+----------+--------
  *        |                    |
  *        |      c, r          |  c+1, r
  *        |                    |
  *  ------+---------+----------+--------
  *                  |
  *      c-1, r+1    |        c, r+1
  *                  |
  *  ----------------+-------------------
  *
  *               c, r + 2
  *
  */
object BrickTileEdge {
  val E = 1
  val SE = 2
  val SW = 3
  val W = 4
  val NW = 5
  val NE = 6
  val all = Seq(E, SE, SW, W, NW, NE)

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

class BrickTiling(val width: Int, val height: Int, val offset: Int) extends Tiling {
  require(width > 0 && height > 0 && offset > 0 && offset < width)

  override def tileCoordinates(col: Int, row: Int): BoundingBox = {
    val y = row * height
    val off = if(row % 2 == 0) { 0 } else { offset }
    val x = col * width + off
    BoundingBox(x, y, x + width, y + height)
  }

  override def edge(from: (Int, Int), to: (Int, Int)): Option[Int] = BrickTileEdge.edge(from, to)
}

class BrickOutline(tiling: BrickTiling) extends TileFactory {
  override def chooseTile(random: RandomX, coord: (Int, Int), tileType: Int, edges: Seq[Int]): String = ""

  override def getTileMaker(gameCfg: GameConfig, name: String, tileType: Int): TileMaker = {
    new BrickOutlineTileMaker(gameCfg, tiling)
  }
}

class BrickOutlineTileMaker(gameCfg: GameConfig, tiling: BrickTiling) extends TileMaker {
  override def makeTile(gameCfg: GameConfig, name: String, tileType: Int, edges: Seq[Int]): SectorGroup = {
    val bb = BoundingBox(0, 0, tiling.width, tiling.height)

    // creating a new map, to create a new sector group
    val map = DMap.createNew()
    val sectorId = ShapePrinter.renderBox(gameCfg, map, bb)
    ShapePrinter.addAnchor(map, sectorId, bb.center)
    SectorGroupBuilder.createSectorGroup(map, SectorGroupProperties.Default, SectorGroupHints.Empty)
  }
}
