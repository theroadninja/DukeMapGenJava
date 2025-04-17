package trn.prefab.experiments

import trn.{HardcodedConfig, RandomX, LineSegmentXY, PointXY, Map => DMap}
import trn.prefab.{SpriteLogicException, MapWriter, GameConfig, DukeConfig}
import trn.render.polygon.PentagonPrinter

/**
  * this is meant to be a re-usable entry point for quick tests
  */
object REUSABLE_EXPERIMENT {

  def main(args: Array[String]): Unit = {

    val gameCfg = BaseExperiment.gameConfig
    val result = tryRun(gameCfg)
    ExpUtil.write(result)

  }

  def tryRun(gameCfg: GameConfig): DMap = {
    val writer = MapWriter(gameCfg)
    try {
      val random = RandomX()
      run(gameCfg, random, writer)
    } catch {
      case e: SpriteLogicException => {
        e.printStackTrace()
        ExpUtil.finish(writer, removeMarkers = false, errorOnWarnings = false)
        writer.outMap
      }
    }
  }

  def run(gameCfg: GameConfig, random: RandomX, writer: MapWriter): DMap = {

    // smallest grid size is 8?
    // val side = new LineSegmentXY(new PointXY(0, 0), new PointXY(2056, 0))
    val side = new LineSegmentXY(new PointXY(4096 + 1024, 0), new PointXY(0, 0))
    PentagonPrinter.printPentagon(writer.getMap, side, Some(128))
    ExpUtil.finish(writer) // auto-add player start
    writer.outMap
  }
}
