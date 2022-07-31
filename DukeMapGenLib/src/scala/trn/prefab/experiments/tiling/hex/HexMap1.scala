package trn.prefab.experiments.tiling.hex

import trn.prefab.experiments.tiling.PythMap1.inputMap
import trn.{HardcodedConfig, MapLoader, RandomX}
import trn.prefab.{GameConfig, PrefabPalette, SectorGroup}
import trn.prefab.experiments.tiling.{HexOutlineTileMaker, HexTiling, PlanNode, TileFactory, TileMaker}

class HexMap1 extends TileFactory {

  val tiling: HexTiling = new HexTiling(12288)

  private def inputMap = HardcodedConfig.getEduke32Path("HEX1.MAP")
  lazy val palette: PrefabPalette = MapLoader.loadPalette(inputMap)

  override def chooseTile(random: RandomX, coord: (Int, Int), tileType: Int, planNode: PlanNode, edges: Seq[Int]): String = random.randomElement(
    HexMap1.all
  )

  override def getTileMaker(gameCfg: GameConfig, name: String, tileType: Int): TileMaker = new Hex1TileMaker(palette)
}

object HexMap1 {
  val StarTile = "STAR"
  val LavaPit = "PIT"
  val CircleHallway = "CIRCLE"
  val all = Seq(StarTile, LavaPit, CircleHallway)

  def apply(): HexMap1 = new HexMap1()
}

class Hex1TileMaker(palette: PrefabPalette) extends TileMaker {
  override def makeTile(gameCfg: GameConfig, name: String, tileType: Int, edges: Seq[Int]): SectorGroup = {
    name match {
      case HexMap1.StarTile => palette.getSG(1)
      case HexMap1.LavaPit => palette.getSG(2)
      case HexMap1.CircleHallway => palette.getSG(3)
    }
  }
}
