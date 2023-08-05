package trn.prefab.experiments.hyperloop

import trn.prefab.experiments.ExpUtil
import trn.prefab.experiments.hyperloop.HyperLoop0.{calcRingAnchors, RingPrinter}
import trn.{HardcodedConfig, ScalaMapLoader}
import trn.prefab.{MapWriter, GameConfig, DukeConfig, SectorGroup}
import trn.prefab.experiments.hyperloop.HyperLoopParser._

object HyperLoop1 {
  val Filename = "loop1.map"

  // TODO - do a cool effect with the cycler
  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    run(gameCfg)
  }

  def run(gameCfg: GameConfig): Unit = {
    val palette = ScalaMapLoader.loadPalette(HardcodedConfig.EDUKE32PATH + Filename, Some(gameCfg))
    val writer = MapWriter(gameCfg)

    val core = palette.getSG(1)
    val innerE = palette.getSG(11)
    val midE = palette.getSG(12)
    val outerE = palette.getSG(13)

    val innerSE = palette.getSG(14)
    val midSE = palette.getSG(15)
    val outerSE = palette.getSG(16)

    val centerToInnerEdgeOfMid = measureWidth(core) / 2 + measureWidth(innerE)
    val midRingAnchors = calcRingAnchors(centerToInnerEdgeOfMid, measureDistToAnchor(midE))

    val ringPrinter = new RingPrinter(writer, midRingAnchors, RingHeadings.East)

    def selectRing(index: Int): SectorGroup = if (index % 2 == 0) {
      midE
    } else {
      midSE
    }

    for (index <- 0 until 16) {
      val psg = ringPrinter.rotateAndPrintClockwise(selectRing(index))
    }

    ringPrinter.autolinkEnds

    ExpUtil.finishAndWrite(writer)
  }
}
