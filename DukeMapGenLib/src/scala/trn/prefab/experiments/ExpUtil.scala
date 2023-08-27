package trn.prefab.experiments

import trn.logic.Tile2d
import trn.{HardcodedConfig, Main}
import trn.prefab.{CompassWriter, MapWriter, SectorGroup}

import trn.{Map => DMap}
/**
  * Utility Functions for Scala Experiments
  */
object ExpUtil {


  def finish(
    writer: MapWriter,
    forcePlayerStart: Boolean = true,
    removeMarkers: Boolean = true,
  ): DMap = {
    // ////////////////////////
    writer.disarmAllSkyTextures()
    writer.setAnyPlayerStart(force = forcePlayerStart)
    if (removeMarkers) {
      writer.sgBuilder.clearMarkers()
    }
    writer.checkSectorCount()
    writer.outMap
  }

  /** convenience method for local testing */
  def write(
    outMap: DMap,
    filename: String = "output.map",
  ): Unit = {
    Main.deployTest(outMap, filename, HardcodedConfig.getEduke32Path(filename))

  }

  def finishAndWrite(
    writer: MapWriter,
    forcePlayerStart: Boolean = true,
    filename: String = "output.map",
    removeMarkers: Boolean = true,
  ): Unit = {
    finish(writer, forcePlayerStart, removeMarkers)
    write(writer.outMap, filename)
    // Main.deployTest(writer.outMap, filename, HardcodedConfig.getEduke32Path(filename))
  }

  /** for older code that is not ready to call finishAndWrite() b/c it doesnt use MapWriter yet */
  def deployMap(outMap: DMap, filename: String = "output.map"): Unit = {
    Main.deployTest(outMap, filename, HardcodedConfig.getEduke32Path(filename))
  }

  //
  def autoReadTile(sg: SectorGroup): Tile2d = Tile2d(
    CompassWriter.east(sg).map(_ => Tile2d.Conn).getOrElse(Tile2d.Blocked),
    CompassWriter.south(sg).map(_ => Tile2d.Conn).getOrElse(Tile2d.Blocked),
    CompassWriter.west(sg).map(_ => Tile2d.Conn).getOrElse(Tile2d.Blocked),
    CompassWriter.north(sg).map(_ => Tile2d.Conn).getOrElse(Tile2d.Blocked),
  )

}
