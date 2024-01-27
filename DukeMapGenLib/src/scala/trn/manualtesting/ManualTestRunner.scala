package trn.manualtesting

import trn.prefab.experiments.ExpUtil
import trn.{HardcodedConfig, RandomX, Map => DMap}
import trn.prefab.{SpriteLogicException, MapWriter, GameConfig, DukeConfig}

object ManualTestRunner {
  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)

    // TODO map contents to a case class
    // val input: DMap = ScalaMapLoader.loadMap(HardcodedConfig.EDUKE32PATH + Filename)
    // val input2: DMap = ScalaMapLoader.loadMap(HardcodedConfig.EDUKE32PATH + OtherFilename)
    // val input3: DMap = ScalaMapLoader.loadMap(HardcodedConfig.EDUKE32PATH + OtherOtherFilename)
    // val input4: DMap = ScalaMapLoader.loadMap(HardcodedConfig.EDUKE32PATH + RandomMoonRoom.Filename)
    // val result = tryRun(gameCfg, SourceMapCollection(input, input2, input3, input4))
    // ExpUtil.write(result)
  }

  def tryRun(gameCfg: GameConfig): DMap = {
    val writer = MapWriter(gameCfg)
    try {
      val random = RandomX()
      // run(gameCfg, random, input, writer)
      ???
    } catch {
      case e: SpriteLogicException => {
        e.printStackTrace()
        ExpUtil.finish(writer, removeMarkers = false, errorOnWarnings = false)
        writer.outMap
      }
    }
  }


}
