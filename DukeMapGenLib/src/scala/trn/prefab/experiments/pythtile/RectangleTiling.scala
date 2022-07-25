package trn.prefab.experiments.pythtile

import trn.RandomX
import trn.prefab.{BoundingBox, GameConfig, MapWriter, PrefabUtils, SectorGroup, SectorGroupBuilder, SectorGroupHints, SectorGroupProperties}
import trn.render.WallPrefab
import trn.{Map => DMap}


// TODO some duplication with Pythagorean Tiling SmallTileEdge
object RectTileEdge {
  val E = 1
  val S = 2
  val W = 3
  val N = 4
  val all = Seq(E, S, W, N)

  def edge(from: (Int, Int), to: (Int, Int)): Option[Int] = {
    val dx = to._1 - from._1
    val dy = to._2 - from._2
    (dx, dy) match {
      case (1, 0) => Some(E)
      case (0, 1) => Some(S)
      case (-1, 0) => Some(W)
      case (0, -1) => Some(N)
      case _ => None
    }
  }
}

// TODO rename to RectTiling
class RectangleTiling(val width: Int, val height: Int) extends Tiling {
  require(width > 0 && height > 0)

  def tileCoordinates(col: Int, row: Int): BoundingBox = {
    val x = col * width
    val y = row * height
    BoundingBox(x, y, x + width, y + height)
  }

  def calcEdges(coord: (Int, Int), neighboors: Seq[(Int, Int)]): Seq[Int] = {
    neighboors.flatMap(n => RectTileEdge.edge(coord, n))
  }

}

class RectOutline(tiling: RectangleTiling) extends TileFactory {
  override def chooseTile(random: RandomX, coord: (Int, Int), tileType: Int, edges: Seq[Int]): String = ""

  override def getTileMaker(gameCfg: GameConfig, name: String, tileType: Int): TileMaker = new RectOutlineTileMaker(gameCfg, tiling)
}

class RectOutlineTileMaker(gameCfg: GameConfig, tiling: RectangleTiling) extends TileMaker {
  override def makeTile(name: String, tileType: Int, edges: Seq[Int]): SectorGroup = {
    val bb = BoundingBox(0, 0, tiling.width, tiling.height)

    // creating a new map, to create a new sector group
    // TODO this code is duplicated in PythOutlineTileMaker
    val map = DMap.createNew()
    val sectorId = ShapePrinter.renderBox(gameCfg, map, bb)
    val marker = MapWriter.newMarkerSprite(sectorId, bb.center.withZ(map.getSector(sectorId).getFloorZ), lotag=PrefabUtils.MarkerSpriteLoTags.ANCHOR)
    map.addSprite(marker)
    val props = new SectorGroupProperties(None, false, None, Seq.empty)
    SectorGroupBuilder.createSectorGroup(map, props, SectorGroupHints.Empty)
  }

}
