package trn.prefab.hypercube

import trn.PointXYZ
import trn.prefab.hypercube.GridManager.Cell

object GridManager {
  type Cell = (Int, Int, Int, Int)

  private def positiveNeighboors(cell: Cell): Seq[Cell] = {
    val (x, y, z, w) = cell
    Seq(
      (x + 1, y, z, w),
      (x, y + 1, z, w),
      (x, y, z + 1, w),
      (x, y, z, w + 1)
    )
  }

  def apply(cellDist: Int, gridSize: Int, zGridSize: Option[Int] = None): GridManager = {
    val half = cellDist * gridSize / 2
    new GridManager(PointXYZ.ZERO.add(new PointXYZ(-half, -half, 0)), cellDist, zGridSize)
  }

  def eachNeighboor(grid: Map[Cell, _])(f: (Cell, Cell) => Unit): Unit = {
    grid.foreach { case (cell: Cell, _) =>
      positiveNeighboors(cell).foreach { neighboor =>
        if(grid.contains(cell) && grid.contains(neighboor)){
          f(cell, neighboor)
        }
      }
    }
  }
}
/**
  * Grid manager for a hyper cube that does NOT offset Z or W values.
  */
class GridManager(
  val origin: PointXYZ,
  val cellDist: Int,
  val cellDistZ: Option[Int]
) {
  val zCellDist: Int = cellDistZ.getOrElse(cellDist)

  def toCoordinates(gridCell: Cell): PointXYZ = {
    val x = gridCell._1 * cellDist
    val y = gridCell._2 * cellDist

    // NOTE:  negative z is higher.  Also need to shift z
    val z = -1 * (gridCell._3 << 4) * zCellDist
    origin.add(new PointXYZ(x, y, z))
  }

  def toCoordinates(x: Int, y: Int, z: Int): PointXYZ = toCoordinates((x, y, z, 0))

}
