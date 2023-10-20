package trn.prefab.experiments.dijkdrop

import trn.prefab.BoundingBox

/**
  * See also SectorGroupPacker
  */
class GridLayout(
  border: BoundingBox,
  colCount: Int, // how many squares horizontally (x direction)
  rowCount: Int, // how many squares vertically (y direction)
) {
  val totalWidth = border.xMax - border.xMin + 1 // [0, 99] -> width=100
  val totalHeight = border.yMax - border.yMin + 1
  require(totalWidth > colCount && colCount > 0)
  require(totalHeight > rowCount && rowCount > 0)
  val cellWidth = totalWidth / colCount
  val cellHeight = totalHeight / rowCount

  /**
    * Calculates the grid coordinates based on the index of an item from a list that is expected
    * to be arranged on the grid left to right, top to bottom.
    *
    * @param index index of an item in a list
    * @return (col, row)
    */
  def toGridCoords(index: Int): (Int, Int) = (index % colCount, index / colCount)

  def bb(gridCoords: (Int, Int)): BoundingBox = {
    val (col, row) = gridCoords
    require(col < colCount && row < rowCount)
    BoundingBox(
      xMin = border.xMin + col * cellWidth,
      yMin = border.yMin + row * cellHeight,
      xMax = border.xMin + (col + 1) * cellWidth - 1,
      yMax = border.yMin + (row + 1) * cellHeight - 1,
    )
  }

  def bbForIndex(index: Int): BoundingBox = bb(toGridCoords(index))


}
