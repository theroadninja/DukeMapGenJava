package trn.prefab.experiments

import trn.logic.Tile2d
import trn.{HardcodedConfig, Main}
import trn.prefab.{CompassWriter, MapWriter, SectorGroup}

/**
  * Utility Functions for Scala Experiments
  */
object ExpUtil {
  def finishAndWrite(writer: MapWriter, forcePlayerStart: Boolean = true, filename: String = "output.map"): Unit = {
    // ////////////////////////
    writer.disarmAllSkyTextures()
    writer.setAnyPlayerStart(force = forcePlayerStart)
    writer.sgBuilder.clearMarkers()
    writer.checkSectorCount()
    Main.deployTest(writer.outMap, filename, HardcodedConfig.getEduke32Path(filename))
  }

  //
  def autoReadTile(sg: SectorGroup): Tile2d = Tile2d(
    CompassWriter.east(sg).map(_ => Tile2d.Conn).getOrElse(Tile2d.Blocked),
    CompassWriter.south(sg).map(_ => Tile2d.Conn).getOrElse(Tile2d.Blocked),
    CompassWriter.west(sg).map(_ => Tile2d.Conn).getOrElse(Tile2d.Blocked),
    CompassWriter.north(sg).map(_ => Tile2d.Conn).getOrElse(Tile2d.Blocked),
  )

}