package trn.prefab.experiments

import trn.bespoke.MoonBase1
import trn.bespoke.moonbase2.{MoonBase2, MoonBase3}
import trn.prefab.{PrefabUtils, DukeConfig}
import trn.{HardcodedConfig, ScalaMapLoader}

import scala.collection.JavaConverters._

/**
  * Creating this to have one place to test all experiments, to make it easier to refactor scanning code and other things.
  */
object ExperimentTester {

  def AllSourceMaps: Seq[String] = Seq(
    // TODO add trn.bespoke.MoonBase1, moonbase2. ... moonbase2 (inside moonbase 2 folder)
    HardcodedConfig.getEduke32Path(Hypercube1B.Filename),
    HardcodedConfig.getEduke32Path(Hypercube2.Filename),
    HardcodedConfig.getEduke32Path(Hypercube4.Filename),
    HardcodedConfig.getDosboxPath(PipeDream.Filename),
    HardcodedConfig.getDosboxPath(PoolExperiment.Filename),
    HardcodedConfig.getEduke32Path(SoundListMap.Filename),
    HardcodedConfig.getEduke32Path(SquareTileMain.Filename),
    HardcodedConfig.getDosboxPath(Sushi.Filename),
    HardcodedConfig.getEduke32Path(Tower.Filename),
    // "Bespoke" maps
    MoonBase1.getMoonMap,
    HardcodedConfig.getEduke32Path(MoonBase2.Filename),
    HardcodedConfig.getEduke32Path(MoonBase3.Filename),
  )

  def main(args: Array[String]): Unit = {

    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)

    AllSourceMaps.foreach { filename =>
      println(s"Loading ${filename}")
      val palette = ScalaMapLoader.loadPalette(filename, Some(gameCfg))

      println(s"   numbered groups: ${palette.numberedSectorGroupCount()}")
      println(s"  anonymous groups: ${palette.anonymousSectorGroupCount()}")
      val multiCount = palette.allSectorGroups().asScala.map { sg =>

        sg.allSprites.filter(s => s.getTex == PrefabUtils.MARKER_SPRITE_TEX && s.getLotag == PrefabUtils.MarkerSpriteLoTags.MULTI_SECTOR)
          .map(_.getLocation.asPointXY())

        // sg.allRedwallConnectors.filter(_.getSectorIds().size() > 1).map(conn => conn.getWallAnchor1())
      }.filter(_.size > 0)
      println(s"  multi sector connectors: ${multiCount}")
    }

  }

}
