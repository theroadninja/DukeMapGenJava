package trn.prefab.experiments.dijkdrop

import trn.prefab.experiments.ExpUtil
import trn.prefab.{FallConnector, BoundingBox, MapWriter, DukeConfig, GameConfig, SectorGroup, ConnectorType, PrefabPalette, SpriteLogicException}
import trn.{HardcodedConfig, RandomX, ScalaMapLoader, BuildConstants, Map => DMap}

case class DropPalette(palette: PrefabPalette) {
  val center = palette.getSG(1)
  val armory = palette.getSG(2)
}


/**
  * See also SectorGroupPacker
  */
class GridLayout(
  border: BoundingBox,
  colCount: Int,  // how many squares horizontally (x direction)
  rowCount: Int,  // how many squares vertically (y direction)
) {
  val totalWidth = border.xMax - border.xMin + 1   // [0, 99] -> width=100
  val totalHeight = border.yMax - border.yMin + 1
  require(totalWidth > colCount && colCount > 0)
  require(totalHeight > rowCount && rowCount > 0)
  val cellWidth = totalWidth / colCount
  val cellHeight = totalHeight / rowCount

  /**
    * Calculates the grid coordinates based on the index of an item from a list that is expected
    * to be arranged on the grid left to right, top to bottom.
    * @param index  index of an item in a list
    * @return  (col, row)
    */
  def toGridCoords(index: Int): (Int, Int) = (index % colCount, index / colCount)

  def bb(gridCoords: (Int, Int)): BoundingBox = {
    val (col, row) = gridCoords
    require(col < colCount && row < rowCount)
    BoundingBox(
      xMin=border.xMin + col * cellWidth,
      yMin=border.yMin + row * cellHeight,
      xMax=border.xMin + (col + 1) * cellWidth - 1,
      yMax=border.yMin + (row + 1) * cellHeight - 1,
    )
  }

  def bbForIndex(index: Int): BoundingBox = bb(toGridCoords(index))



}

object Dijkdrop {
  val Filename = "dijkdrop.map"

  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    val input: DMap = ScalaMapLoader.loadMap(HardcodedConfig.EDUKE32PATH + Filename)
    val result = tryRun(gameCfg, input)
    ExpUtil.write(result)
  }

  def tryRun(gameCfg: GameConfig, input: DMap): DMap = {
    val writer = MapWriter(gameCfg)
    try {
      val random = RandomX()
      run(gameCfg, random, input, writer)
    } catch {
      case e: SpriteLogicException => {
        e.printStackTrace()
        ExpUtil.finish(writer, removeMarkers = false, errorOnWarnings = false)
        writer.outMap
      }
    }
  }

  def linkedByFall(sgA: SectorGroup, connA: Int, sgB: SectorGroup, connB: Int): (SectorGroup, SectorGroup) = {

    // TODO this wont work, because it doesnt re-scan after the modifications
    // sgA.withModifiedSectors()

    ???
  }

  def run(gameCfg: GameConfig, random: RandomX, inputMap: DMap, writer: MapWriter): DMap = {
    val palette = DropPalette(ScalaMapLoader.paletteFromMap(gameCfg, inputMap))

    val layout = new GridLayout(BuildConstants.MapBounds, 6, 6)

    val groups = Seq(palette.center,
      palette.armory,
    )

    val fallConns = palette.center.allConnectors.filter(_.getConnectorType == ConnectorType.FALL_CONNECTOR).map(_.asInstanceOf[FallConnector])

    groups.zipWithIndex.foreach {
      case (sg, index) => {
        val bb = layout.bbForIndex(index)
        writer.tryPasteInside(sg, bb).get
      }
    }

    ExpUtil.finish(writer)
    writer.outMap
  }
}
