package trn.prefab.experiments.subway

import trn.{PointXYZ, RandomX, HardcodedConfig, ScalaMapLoader, Sprite, Map => DMap}
import trn.duke.TextureList
import trn.math.SnapAngle
import trn.prefab.experiments.ExpUtil
import trn.prefab.{MapWriter, DukeConfig, GameConfig, SectorGroup, RedwallConnector, PrefabPalette, PastedSectorGroup, SpriteLogicException}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  *
  *           101
  *     /-------------\
  *     |     102     |
  *     |             |
  * 103 | 104     105 | 106
  *     |             |
  *     |             |
  *     |             |
  * 107 | 108     XXX | XXX
  *     |             |
  *     |     109     |
  *     \-------------/
  *           110
  *
  *
  *
  *
  */
object ConnectionIds {

  val Track: Seq[Int] = Seq(101, 102, 103, 104, 105, 106, 107, 108, 109, 110)

  /** track <-> platform_edge */
  val TypeA: Int = 200

  val ExpectedLengthA = 13312 // Mapster32 shows 13313?

  /** platform_edge <-> platform_area */
  val TypeB: Int = 201

  /** platform_area <-> door */
  val TypeC: Int = 202

  /** door <-> special_area */
  val TypeD: Int = 203

  /**
    * on the inside, the end pieces overlap with their neighboors
    */
  val Conflicts: Set[(Int, Int)] = Set(
    (102, 104),
    (102, 105),
    (108, 109),
  )

  def conflict(a: Int, b: Int): Boolean = if(a < b){ Conflicts(a, b) }else{ Conflicts(b, a)}

}



case class GenerationResult(
  success: Boolean,
  outMap: DMap,
  error: Option[Exception],
){

}


/**
  * Very simple subway with 1 loop, hardcoded platform connections
  */
object Subway1 {
  val Filename = "subway3.map"

  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)

    val random = new RandomX()

    val filename = HardcodedConfig.EDUKE32PATH + Filename
    // val palette = ScalaMapLoader.loadPalette(HardcodedConfig.EDUKE32PATH + Filename, Some(gameCfg))
    val input: DMap = ScalaMapLoader.loadMap(HardcodedConfig.EDUKE32PATH + Filename)
    val result: GenerationResult = tryRun(gameCfg, input)
    if(result.success){
      println("Generation Succeeded")
    }else{
      println("Generation Failed")
      result.error.foreach(_.printStackTrace())
    }
    ExpUtil.write(result.outMap)
  }

  // trying to experiment with the interface the lambda will need
  def tryRun(gameCfg: GameConfig, inputMap: DMap): GenerationResult = {
    val writer = MapWriter(gameCfg)
    try {
      val outMap = run(gameCfg, inputMap, writer)
      GenerationResult(true, outMap, None)
    } catch {
      case e: SpriteLogicException => {
        e.printStackTrace()
        ExpUtil.finish(writer, removeMarkers=false)
        GenerationResult(false, writer.outMap, Some(e))
      }
    }
  }

  // TODO there is an incoming "TODO" link from Placement.placements()
  def rotateAndPasteToTrack(
    writer: MapWriter,
    trackConn: RedwallConnector,
    platformEdge: PlatformEdge,
  ): PastedSectorGroup = {

    val zAdjust = 8192

    // TODO maybe the real solution is auto rotation when combining SectorGroups ?

    writer.rotatePasteAndLink(
      trackConn,
      platformEdge,
      Seq.empty,
    ){ rotatedPlatformEdge =>
      (rotatedPlatformEdge.connToTrack.withAnchorZAdjusted(zAdjust), rotatedPlatformEdge.sg)

    }

    // val (_, rotatedEdge) = SnapAngle.rotateUntil2(platformEdge) { edge =>
    //   writer.canPlaceAndConnect(trackConn, edge.connToTrack, edge.sg, false)
    // }.getOrElse(throw new SpriteLogicException("could not match edge to track!"))
    // writer.pasteAndLink(trackConn, rotatedEdge.sg, rotatedEdge.connToTrack.withAnchorZAdjusted(8192), Seq.empty)
  }

  def run(gameCfg: GameConfig, inputMap: DMap, writer: MapWriter): DMap = {
    val palette = ScalaMapLoader.paletteFromMap(gameCfg, inputMap)
    val subwayPal = SubwayPalette1(palette)

    val trackPSG = writer.pasteSectorGroupAt(subwayPal.trackSg, PointXYZ.ZERO)

    val psgConn = trackPSG.getRedwallConnector(ConnectionIds.Track(0))
    val edge = subwayPal.platformEdgesWithoutGates(0)
    rotateAndPasteToTrack(writer, psgConn, edge)


    ExpUtil.finish(writer)
  }

}