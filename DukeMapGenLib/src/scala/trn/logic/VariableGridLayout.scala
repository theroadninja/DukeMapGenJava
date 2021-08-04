package trn.logic

import trn.PointXY
import trn.prefab.BoundingBox

/**
  * Stores info about a grid where each row and column can be different heights/widths.  Also allows grid and column
  * ids to be negative, or not start at 0
  *
  *      -3   -2    -1    0   1    2
  *     +--+------+---+------+-+-------+
  *  2  |         |   |      | |       |
  *     |         |   |      | |       |
  *     +--+------+---+------+-+-------+
  *  3  |         |   |      | |       |
  *     +--+------+---+------+-+-------+
  *     |         |   |      | |       |
  *  4  |         |   |      | |       |
  *     |         |   |      | |       |
  *     +--+------+---+------+-+-------+
  *
  * column and row ids can be negative (the edge of the layout starts at the smallest values)
  */
case class VariableGridLayout(
  private[logic] val colWidths: Seq[Int],
  private[logic] val rowHeights: Seq[Int],
  private[logic] val colOffset: Int,  // seq_index = column_id - offset   (so offset is negative
  private[logic] val rowOffset: Int,
  colSpace: Int,
  rowSpace: Int
) {
  /** left x coordinate of each column */
  private[logic] val colLeft = colWidths.scanLeft(0){ (colwidth, total) => total + colwidth + colSpace}.dropRight(1)

  /** top y coordinate of each row */
  private[logic] val rowTop = rowHeights.scanLeft(0){ (rowHeight, total) => total + rowHeight + rowSpace}.dropRight(1)

  private[logic] def colIndex(columnId: Int): Int = columnId - colOffset
  private[logic] def rowIndex(rowId: Int): Int = rowId - rowOffset

  /** width of the given column, identified by "column id" */
  def colWidth(columnId: Int): Int = colWidths(colIndex(columnId))

  /** height of the given row, identified by "row id" */
  def rowHeight(rowId: Int): Int = rowHeights(rowIndex(rowId))

  /** width of the entire grid */
  val width = colWidths.sum + (colWidths.length -1) * colSpace
  require(colLeft.last + colWidths.last == width)

  /** height of the entire grid */
  val height = rowHeights.sum + (rowHeights.length - 1) * rowSpace
  require(rowTop.last + rowHeights.last == height)

  /**
    * @returns the BoundingBox of the whole grid, with xmin and ymin set to 0 since this grid doesnt have a location
    */
  lazy val boundingBox = BoundingBox(0, 0, width, height)

  lazy val center: PointXY = new PointXY(width/2, height/2)

  def boundingBox(colId: Int, rowId: Int): BoundingBox = {
    val ci = colIndex(colId)
    val ri = rowIndex(rowId)
    val left = colLeft(ci)
    val top = rowTop(ri)
    BoundingBox(left, top, left + colWidths(ci), top + rowHeights(ri))
  }
}

object VariableGridLayout {

  /**
    * @param colWidths map of "column id" to width of that column
    * @param rowHeights map fo "row id" to width of that row
    * @param colSpace size of gap between each column
    * @param rowSpace size of gap between each row
    * @return
    */
  def apply(colWidths: Map[Int, Int], rowHeights: Map[Int, Int], colSpace: Int, rowSpace: Int): VariableGridLayout = {
    val columnIds = colWidths.keySet.toSeq.sorted
    val rowIds = rowHeights.keySet.toSeq.sorted
    require(columnIds.length == columnIds.last - columnIds.head + 1)
    require(rowIds.length == rowIds.last - rowIds.head + 1)
    require(! colWidths.values.exists(_ < 0))
    require(! rowHeights.values.exists(_ < 0))
    VariableGridLayout(columnIds.map(colWidths), rowIds.map(rowHeights), columnIds(0), rowIds(0), colSpace, rowSpace)
  }
}
