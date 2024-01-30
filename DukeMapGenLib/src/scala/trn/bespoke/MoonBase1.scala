package trn.bespoke

import trn.duke.TextureList
import trn.{PointXY, Sector, ScalaMapLoader, Main, PlayerStart, BuildConstants, PointXYZ, WallView, HardcodedConfig, LineSegmentXY, Map => DMap}
import trn.prefab.{SimpleSectorGroupPacker, MapWriter, DukeConfig, CompoundGroup, GameConfig, SectorGroup, RedwallConnector, PasteOptions, PastedSectorGroup, PrefabPalette, SectorGroupPacker, SpriteLogicException, Marker}

import scala.collection.JavaConverters._

object Space {
  val Door = 1 // sector group of split door with circles
}
/**
  * This creates a "moon base" themed map, aiming to recreat the levels in Duke3D Episode 2, especially L6, L7, L8.
  *
  * It is a "bespoke" generator, meaning it uses hardcoded assets and doesnt take in any input.
  *
  * Uses assets from space.map and moon1.map
  */
object MoonBase1 {

  def getSpaceMap: String = HardcodedConfig.getMapDataPath("SPACE.MAP")

  def getMoonMap: String = HardcodedConfig.getDosboxPath("MOON1.MAP")


  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    run(gameCfg)
  }

  def run(gameCfg: GameConfig): Unit = {
    val packer = SimpleSectorGroupPacker(
      new PointXY(BuildConstants.MIN_X, 0),
      new PointXY(0, BuildConstants.MAX_Y)
    )
    val writer = MapWriter(gameCfg, Some(packer))
    try{
      // 1. this has more:
      run(gameCfg, writer)

      // 2. this also worked (dont remember what I was doing):
      // runTest(gameCfg, writer)

      Main.deployTest(writer.outMap)
    }catch{
      case ex: Throwable => {
        ex.printStackTrace()
        println("map generation failed")
        writer.setAnyPlayerStart(true)
        Main.deployTest(writer.outMap, "error.map")
      }

    }
  }

  def pasteInSequence(writer: MapWriter, startPsg: PastedSectorGroup, startConn: RedwallConnector, groups: Seq[SectorGroup]): PastedSectorGroup = {
    SpriteLogicException.throwIf(groups.exists(_.allRedwallConnectors.isEmpty), "Sector group missing connector")

    val (psg, _) = groups.foldLeft(  (startPsg, Option(startConn))){ case ((psg, conn), sg) =>
      println(s"pasting sg ${sg.getGroupId}")
      val psg2 = writer.tryPasteConnectedTo(psg, conn.get, sg, PasteOptions()).get
      val conn2 = psg2.unlinkedRedwallConnectors.headOption
      (psg2, conn2)
    }
    psg
  }

  def runTest(gameCfg: GameConfig, writer: MapWriter): Unit = {
    val spacePalette = ScalaMapLoader.loadPalette(getSpaceMap)
    val moonPalette = ScalaMapLoader.loadPalette(getMoonMap)

    // TODO: maybe these belong in some kind of custom, scenario-specific palette
    val doors = Seq(1, 4).map(spacePalette.getSG)
    val startingAreas = Seq(2).map(moonPalette.getSG(_))

    val center = writer.pasteSectorGroupAt(moonPalette.getSectorGroup(1), PointXYZ.ZERO)
    val openConns = writer.randomShuffle(center.redwallConnectors).toSeq


    val sgs0 = Seq(
      writer.randomElement(doors),
      writer.randomElement(startingAreas)
    )
    val psg0 = pasteInSequence(writer, center, openConns(0), sgs0)

    val elevatorTop = moonPalette.getSectorGroup(12)
    val elevator = CompoundGroup(
      moonPalette.getSectorGroup(12),
      moonPalette.getTeleChildren(12).asScala
    )
    require(elevator.teleportChildGroups.size  == 1)

    // elevator.teleportChildGroups(0).allRotations.foreach(r => writer.pasteAnywhere(r))

    writer.tryPasteConnectedTo(center, elevator, PasteOptions())

    writer.disarmAllSkyTextures()
    writer.setAnyPlayerStart(force = true)
    writer.sgBuilder.clearMarkers()
    writer.checkSectorCount()
  }


  def run(gameCfg: GameConfig, writer: MapWriter): Unit = {

    // TODO - compare space.map in proj folder and in workspace and fail fast if they are different (proj version
    // out of date)

    val spacePalette = ScalaMapLoader.loadPalette(getSpaceMap)
    val moonPalette = ScalaMapLoader.loadPalette(getMoonMap)

    val center = writer.pasteSectorGroupAt(moonPalette.getSectorGroup(1), PointXYZ.ZERO)

    val doors = Seq(1, 4).map(spacePalette.getSG)
    val doorsWithLocks = Seq(2, 5).map(spacePalette.getSG)
    val startingAreas = Seq(2).map(moonPalette.getSG(_))
    val hallways = Seq(3, 4, 6, 7, 10).map(moonPalette.getSG(_))
    val setPieces = Seq(5, 8, 11).map(moonPalette.getSG(_))
    val endSetPieces = setPieces.filter(_.allRedwallConnectors.size > 1)
    val endPieces = moonPalette.allSectorGroups.asScala.filter(_.containsSprite(s => s.getTex == TextureList.Switches.NUKE_BUTTON))


    def withKey(sg: SectorGroup, keyColor: Int): SectorGroup = {
      require(DukeConfig.KeyColors.contains(keyColor))

      val sg2 = sg.copy
      val keysprite = sg2.allSprites.find(s => Marker.isMarker(s, Marker.Lotags.ITEM)).get
      keysprite.setTexture(TextureList.Items.KEY)
      keysprite.setLotag(0)
      keysprite.setPal(keyColor)
      sg2
    }

    // change all locks to be the specified color
    def withLockColor(sg: SectorGroup, keyColor: Int): SectorGroup = {
      require(DukeConfig.KeyColors.contains(keyColor))

      val sg2 = sg.copy
      val locks = sg2.allSprites.filter(s => Seq(TextureList.Switches.ACCESS_SWITCH, TextureList.Switches.ACCESS_SWITCH_2).contains(s.getTex))
      locks.foreach(s => s.setPal(keyColor))
      sg2
    }

    val openConns = writer.randomShuffle(center.redwallConnectors).toSeq
    require(openConns.size == 4)

    val endingBranch = writer.randomElement(Seq(1, 2, 3))


    // first branch: starting area
    //writer.tryPasteConnectedTo(center, openConns(0), writer.randomElement(startingAreas), PasteOptions())
    val sgs0 = Seq(
      writer.randomElement(doors),
      writer.randomElement(startingAreas)
    )
    val psg0 = pasteInSequence(writer, center, openConns(0), sgs0)


    // second branch:  not locked // TODO - option to just give it a list of sector groups ...
    val setPieces1 = if(endingBranch == 1){ endSetPieces }else{ setPieces }
    val sgs1 = Seq(
      writer.randomElement(doors),
      writer.randomElement(hallways),
      withKey(writer.randomElement(setPieces1), DukeConfig.KeyColors(0))
    )
    val psg1 = pasteInSequence(writer, center, openConns(1), sgs1)

    // third branch
    val setPieces2 = if(endingBranch == 2){ endSetPieces}else{ setPieces }
    val sgs2 = Seq(
      withLockColor(writer.randomElement(doorsWithLocks), DukeConfig.KeyColors(0)),
      writer.randomElement(hallways),
      withKey(writer.randomElement(setPieces2), DukeConfig.KeyColors(1))
    )
    val psg2 = pasteInSequence(writer, center, openConns(2), sgs2)

    // fourth branch
    val setPieces3 = if(endingBranch == 3){ endSetPieces }else{ setPieces }
    val sgs3 = Seq(
      withLockColor(writer.randomElement(doorsWithLocks), DukeConfig.KeyColors(1)),
      writer.randomElement(hallways),
      withKey(writer.randomElement(setPieces3), DukeConfig.KeyColors(2))
    )
    val psg3 = pasteInSequence(writer, center, openConns(3), sgs3)

    val endingStart = Seq(psg0, psg1, psg2, psg3)(endingBranch)
    pasteInSequence(writer, endingStart, endingStart.unlinkedRedwallConnectors.head, Seq(
      withLockColor(writer.randomElement(doorsWithLocks), DukeConfig.KeyColors(2)),
      writer.randomElement(endPieces)
    ))
    // TODO - add a hallway where random walls open up (dont know which ones)



    writer.disarmAllSkyTextures()
    writer.setAnyPlayerStart(force = true)
    writer.sgBuilder.clearMarkers()
    writer.checkSectorCount()

  }
}
