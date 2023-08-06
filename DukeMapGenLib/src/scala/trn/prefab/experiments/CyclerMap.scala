package trn.prefab.experiments

import trn.duke.TextureList
import trn.prefab.experiments.hyperloop.SpriteFactory
import trn.{Sprite, HardcodedConfig, ScalaMapLoader, PointXYZ}
import trn.prefab.{BoundingBox, MapWriter, DukeConfig, GameConfig, PastedSectorGroup}
import scala.collection.JavaConverters._

object CyclerMap {

  val Filename = "cycler.map"

  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    run(gameCfg)
  }

  def findCycler(psg: PastedSectorGroup, lotag: Int): Sprite = {
    psg.allSprites.find(s => s.getTex == TextureList.CYCLER && s.getLotag == lotag).getOrElse(throw new Exception(s"Cant find cycler with lotag ${lotag}"))
  }

  def run(gameCfg: GameConfig): Unit = {
    val palette = ScalaMapLoader.loadPalette(HardcodedConfig.EDUKE32PATH + Filename, Some(gameCfg))
    val writer = MapWriter(gameCfg)
    val sg = palette.getSG(1)
    val psg = writer.pasteSectorGroupAt(sg, PointXYZ.ZERO)
    val CyclerCount = 30 // # of cyclers in the map

    // To understand these parameters, see Cycler.md
    setExperiment(
      psg,
      offsetDelta = 2048 / CyclerCount, // 2048 for one beam, 4096 for 2, 6144 for 3...
      pulseSpeed = 135,
    )
    ExpUtil.finishAndWrite(writer)
  }

  def setExperiment(
    psg: PastedSectorGroup,
    offsetDelta: Int, // CYCLER lotag
    pulseSpeed: Int, // GPSPEED lotag
  ): Unit = {
    println(s"offsetDelta=${offsetDelta} pulseSpeed=${pulseSpeed}")

    // CYCLER
    // lotag = pulse offset
    // hitag = optional channel to match with switch
    // angle =  must be north
    // GPSPEED
    // lotag = how fast to pulse
    val cyclers = (1 to 30).map(i => findCycler(psg, i))

    for (i <- 0 until cyclers.size) {
      val offset = i * offsetDelta
      val cycler = cyclers(i)
      cycler.setLotag(offset)

      val sectorId = cycler.getSectorId
      val walls = psg.getOuterWallLoop(sectorId)
      val bb = BoundingBox(walls.flatMap(_.points().asScala))
      val z = psg.map.getSector(sectorId).getFloorZ
      val gpspeed = SpriteFactory.sprite(bb.center.withZ(z), sectorId, TextureList.GPSSPEED, lotag = pulseSpeed)
      psg.map.addSprite(gpspeed)
    }

  }

}
