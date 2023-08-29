package trn.prefab.experiments.subway

import trn.{PointXYZ, RandomX, HardcodedConfig, ScalaMapLoader, Sprite, Map => DMap}
import trn.duke.TextureList
import trn.math.SnapAngle
import trn.prefab.experiments.ExpUtil
import trn.prefab.experiments.hyperloop.Item
import trn.prefab.{MapWriter, DukeConfig, EnemyMarker, GameConfig, SectorGroup, RedwallConnector, PrefabPalette, PastedSectorGroup, SpriteLogicException, Marker}

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

  /** map of [track conn id -> lotag of station locator] */
  val ConnToLocator: Map[Int, Int] = Map(
    101 -> 8,
    102 -> 8,
    103 -> 16,
    104 -> 16,
    105 -> 36,
    106 -> 36,
    107 -> 18,
    108 -> 18,
    109 -> 27,
    110 -> 27,


  )

  def conflict(a: Int, b: Int): Boolean = if(a < b){ Conflicts(a, b) }else{ Conflicts(b, a)}

  def hasConflict(newConn: Int, existing: Set[Int]): Boolean = existing.exists(b => conflict(newConn, b))
}

object AreaTypes {
  val Start = "S"
  val Key1 = "K1"
  val Gate1Key2 = "G2K2"
  val Gate2End = "K2E"

  val All: Seq[String] = Seq(Start, Key1, Gate1Key2, Gate2End)
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
      val random = RandomX()
      val outMap = run(gameCfg, random, inputMap, writer)
      GenerationResult(true, outMap, None)
    } catch {
      case e: SpriteLogicException => {
        e.printStackTrace()
        ExpUtil.finish(writer, removeMarkers=false, errorOnWarnings=false)
        GenerationResult(false, writer.outMap, Some(e))
      }
    }
  }


  def rotateAndPasteToTrack2(
    writer: MapWriter,
    trackConn: RedwallConnector,
    newSg: SectorGroup,
  )(
    postRotateFn: SectorGroup => (RedwallConnector, SectorGroup),
  ): PastedSectorGroup = {
    // val zAdjust = 8192
    writer.rotatePasteAndLink(
      trackConn,
      newSg,
      Seq.empty,
      false, // the overlap detection is primitive and will prevent pasting inside the track
    ) (postRotateFn)
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
      false, // the overlap detection is primitive and will prevent pasting inside the track
    ){ rotatedPlatformEdge =>
      (rotatedPlatformEdge.connToTrack.withAnchorZAdjusted(zAdjust), rotatedPlatformEdge.sg)
    }

    // val (_, rotatedEdge) = SnapAngle.rotateUntil2(platformEdge) { edge =>
    //   writer.canPlaceAndConnect(trackConn, edge.connToTrack, edge.sg, false)
    // }.getOrElse(throw new SpriteLogicException("could not match edge to track!"))
    // writer.pasteAndLink(trackConn, rotatedEdge.sg, rotatedEdge.connToTrack.withAnchorZAdjusted(8192), Seq.empty)
  }

  def chooseTrackLocations(random: RandomX, count: Int): Seq[Int] = {
    val all = ConnectionIds.Track.toSet
    val chosen = mutable.Set[Int]()
    for(_ <- 0 until count){
      val next = random.randomElement(all.diff(chosen).filterNot(ConnectionIds.hasConflict(_, chosen.toSet)))
      chosen.add(next)
    }
    random.shuffle(chosen).toSeq
  }

  def attach(gameCfg: GameConfig, sgA: SectorGroup, sgB: SectorGroup, sharedConnId: Int): SectorGroup  = {
    val connA = sgA.getRedwallConnector(sharedConnId)
    sgA.withGroupAttachedAutoRotate(gameCfg, connA, sgB){ sg =>
      sg.getRedwallConnector(sharedConnId)
    }
  }

  def connectAll(
    gameCfg: GameConfig,
    platformEdge: SectorGroup,
    platformArea: SectorGroup,
    door: SectorGroup,
    specialArea: SectorGroup,
  ): SectorGroup = {
    val sg = attach(gameCfg, platformEdge, platformArea, ConnectionIds.TypeB)
    val sg2 = attach(gameCfg, sg, door, ConnectionIds.TypeC)
    attach(gameCfg, sg2, specialArea, ConnectionIds.TypeD)
  }

  def createArea(gameCfg: GameConfig, random: RandomX, pal: SubwayPalette1, area: String): SectorGroup = {
    // gate can be on the platform itself, or a locked door
    // val gateAtPlatform = random.nextBool()  TODO
    val key1 = Item.BlueKey
    val key2 = Item.RedKey
    area match {
      case AreaTypes.Start => {
        val edge = random.randomElement(pal.platformEdgesWithoutGates)
        val area = random.randomElement(pal.platformAreas)
        val door = random.randomElement(pal.doorsWithoutGates)
        val start = random.randomElement(pal.specialAreas.filter{a => a.isStart})
        connectAll(gameCfg, edge.sg, area.sg, door.sg, start.sg)
      }
      case AreaTypes.Key1 => {
        val edge = random.randomElement(pal.platformEdgesWithoutGates)
        val area = random.randomElement(pal.platformAreas)
        val door = random.randomElement(pal.doorsWithoutGates)
        val key = random.randomElement(pal.specialAreas.filter(_.isItem))
        connectAll(gameCfg, edge.sg, area.sg, door.sg, key.sg.withItem(key1.tex, key1.pal))
      }
      case AreaTypes.Gate1Key2 => {
        // TODO other version of gates (not at platform edge)
        val edge = random.randomElement(pal.platformEdgesWithGates)
        val area = random.randomElement(pal.platformAreas)
        val door = random.randomElement(pal.doorsWithoutGates)
        val key = random.randomElement(pal.specialAreas.filter(_.isItem))
        connectAll(gameCfg, edge.sg.withKeyLockColor(gameCfg, key1.pal), area.sg, door.sg, key.sg.withItem(key2.tex, key2.pal))
      }
      case AreaTypes.Gate2End => {
        val edge = random.randomElement(pal.platformEdgesWithGates)
        val area = random.randomElement(pal.platformAreas)
        val door = random.randomElement(pal.doorsWithoutGates)
        val end = random.randomElement(pal.specialAreas.filter(_.isEnd))
        connectAll(gameCfg, edge.sg.withKeyLockColor(gameCfg, key2.pal), area.sg, door.sg, end.sg)
      }
    }
  }

  def setTrainPauses(trackSG: SectorGroup, chosenConnectors: Seq[Int]): SectorGroup = {
    val cp = trackSG.copy
    val allLocators = ConnectionIds.ConnToLocator.values
    val usedLocators = chosenConnectors.map(ConnectionIds.ConnToLocator)
    allLocators.foreach { lotag =>
      val locator = cp.allSprites.find(s => s.getTex == TextureList.LOCATORS && s.getLotag == lotag).get
      if(usedLocators.contains(lotag)){
        locator.setHiTag(1)
      }else{
        locator.setHiTag(0)
      }
    }
    cp
  }

  def run(gameCfg: GameConfig, random: RandomX, inputMap: DMap, writer: MapWriter): DMap = {
    val palette = ScalaMapLoader.paletteFromMap(gameCfg, inputMap)
    val subwayPal = SubwayPalette1(palette)

    val trackLocations = chooseTrackLocations(random, 4)
    val trackSG = setTrainPauses(subwayPal.trackSg, trackLocations)
    val trackPSG = writer.pasteSectorGroupAt(trackSG, PointXYZ.ZERO)

    // Areas:
    // - start
    // - k1
    // - g1, k2
    // - k2, end

    // val area = createArea(gameCfg, random, subwayPal, AreaTypes.Start)

    trackLocations.zip(AreaTypes.All).foreach { case (trackConnId, areaType) =>
      val trackConn = trackPSG.getRedwallConnector(trackConnId)
      val area = createArea(gameCfg, random, subwayPal, areaType)
      doEnemyMarkers(random, area)



      rotateAndPasteToTrack2(writer, trackConn, area){ sg =>
        val zAdjust = 8192
        val conn = sg.getRedwallConnector(ConnectionIds.TypeA).withAnchorZAdjusted(zAdjust)
        (conn, sg)
      }
    }

    // trackLocations.foreach { connId =>
    //   val psgConn = trackPSG.getRedwallConnector(connId)
    //   rotateAndPasteToTrack2(writer, psgConn, area){ sg =>
    //     val conn = sg.getRedwallConnector(ConnectionIds.TypeA)
    //     (conn, sg)
    //   }
    // }

    ExpUtil.finish(writer)
  }

  def doEnemyMarkers(random: RandomX, sg: SectorGroup): Unit = {
    sg.allSprites.filter(s => Marker.isMarker(s, Marker.Lotags.ENEMY)).foreach { markerSprite =>
      val enemyMarker = EnemyMarker(markerSprite)
      if(enemyMarker.turnIntoRespawn){
        throw new SpriteLogicException("Enemy Marker turn-into-respawn feature not implemented yet")
      }
      if (enemyMarker.locationFuzzing) {
        throw new SpriteLogicException("Enemy Marker location fuzzing not implemented yet")
      }
      val enemy = random.randomElement(enemyMarker.enemyList)
      enemy.writeTo(markerSprite)
    }

  }

}