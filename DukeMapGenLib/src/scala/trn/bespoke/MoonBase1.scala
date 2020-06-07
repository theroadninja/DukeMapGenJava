package trn.bespoke

import trn.{BuildConstants, HardcodedConfig, LineSegmentXY, Main, MapLoader, PlayerStart, PointXY, PointXYZ, Sector, WallView, Map => DMap}
import trn.prefab.{GameConfig, MapWriter, PasteOptions, PrefabPalette}

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

  def run(gameCfg: GameConfig): Unit = {

    // TODO - compare space.map in proj folder and in workspace and fail fast if they are different (proj version
    // out of date)

    val spacePalette = MapLoader.loadPalette(getSpaceMap)
    val moonPalette = MapLoader.loadPalette(getMoonMap)

    val writer = MapWriter()

    val (center, _) = writer.sgBuilder.pasteSectorGroup2(moonPalette.getSectorGroup(1), PointXYZ.ZERO)

    writer.tryPasteConnectedTo(center, spacePalette.getSG(Space.Door), PasteOptions())


    writer.setAnyPlayerStart(force = true)
    writer.sgBuilder.clearMarkers()
    writer.checkSectorCount()
    Main.deployTest(writer.outMap)
  }
}
