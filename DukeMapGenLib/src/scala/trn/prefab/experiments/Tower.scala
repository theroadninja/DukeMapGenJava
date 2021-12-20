package trn.prefab.experiments

import trn.{BuildConstants, HardcodedConfig, MapLoader, PointXYZ, Map => DMap}
import trn.prefab.{DukeConfig, GameConfig, MapWriter, SectorGroup}
import trn.render.{HorizontalBrush, MiscPrinter, Texture, TextureUtil, TowerStairPrinter, WallAnchor, WallPrefab, WallSectorAnchor}
import trn.PointImplicits._

import scala.collection.JavaConverters._

/**
  * Requirements:
  * - redwall conns must have 1 wall each.
  *
  * @param sg
  */
case class Landing(sg: SectorGroup){
}


object Tower {

  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    val writer = MapWriter(gameCfg)
    try {
      run(gameCfg, writer)
    } catch {
      case e => {
        writer.setAnyPlayerStart(true)
        ExpUtil.deployMap(writer.outMap, "error.map")
        // Main.deployTest(writer.outMap, "error.map", HardcodedConfig.getEduke32Path("error.map"))
        throw e
      }
    }
  }
  def run(gameCfg: GameConfig, writer: MapWriter): Unit = {
    val palette = MapLoader.loadPalette(HardcodedConfig.getEduke32Path("tower1.map"))

    val landing = Landing(palette.getSG(3))

    // TODO should have hardcoded stairs in case the level doesnt specify any
    val psg = writer.pasteSectorGroupAt(landing.sg, PointXYZ.ZERO)




    val conn1 = psg.getRedwallConnectorsById(1).get(0)
    val startSector = conn1.getSectorId
    val anchor1 = WallSectorAnchor(writer.getWallAnchor(conn1), startSector)

    val sideWall = WallPrefab(gameCfg.tex(349)) // TODO read from landing
    val stairFloor = HorizontalBrush(755).withRelative(true).withSmaller(true)
    val stairCeil = HorizontalBrush(437)

    TowerStairPrinter.printSomeStairs(writer.outMap, anchor1, sideWall, stairFloor, stairCeil)

    ExpUtil.finishAndWrite(writer, forcePlayerStart = false)
  }


}
