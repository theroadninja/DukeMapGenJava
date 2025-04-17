package trn.prefab.experiments

import trn.prefab.{GameConfig, DukeConfig}
import trn.{HardcodedConfig, ScalaMapLoader}

object BaseExperiment {

  lazy val gameConfig: GameConfig = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)

  def getMapLoader(): ScalaMapLoader = ScalaMapLoader(HardcodedConfig.EDUKE32PATH)

}
