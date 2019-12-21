package trn.prefab.hypercube

import trn.PointXYZ
import trn.prefab.Heading
import trn.prefab.hypercube.GridManager.Cell

import scala.collection.mutable

object GridCell {
  def apply(cell: Cell): GridCell = GridCell(cell._1, cell._2, cell._3, cell._4)
}

case class GridCell(x: Int, y: Int, z: Int, w: Int){
  def add(cell: Cell): GridCell = GridCell(
    x + cell._1,
    y + cell._2,
    z + cell._3,
    w + cell._4
  )

  def asTuple: Cell = (x, y, z, w)
}

object GridManager {
  type Cell = (Int, Int, Int, Int)

  // private[hypercube] def contains(c: Cell, i: Int): Boolean = c._1 == i || c._2 == i || c._3 == i || c._4 == i

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
    // gridSize is length of side in cells
    val half = cellDist * gridSize / 2
    new GridManager(PointXYZ.ZERO.add(new PointXYZ(-half, -half, 0)), gridSize, cellDist, zGridSize)
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

  /**
    * @return the minimum number of times you must rotate currentHeading CW until it matches a rotation in rotations
    */
  def minRotationsToMatch(rotations: Seq[Int], currentHeading: Int): Int = {
    if(rotations.contains(currentHeading)){
      0
    }else if(rotations.contains(Heading.rotateCW(currentHeading))){
      1
    }else if(rotations.contains(Heading.rotateCW(Heading.rotateCW(currentHeading)))){
      2
    }else{
      3
    }
  }
}
/**
  * Grid manager for a hyper cube that does NOT offset Z or W values.
  */
class GridManager(
  val origin: PointXYZ,
  val sideLength: Int,
  val cellDist: Int,
  val cellDistZ: Option[Int]
) {
  val zCellDist: Int = cellDistZ.getOrElse(cellDist)

  lazy val allCells: Set[Cell] = {
    (0 until sideLength).flatMap { x =>
      (0 until sideLength).flatMap { y =>
        (0 until sideLength).flatMap { z =>
          (0 until sideLength).map { w =>
            (x, y, z, w)
          }
        }
      }
    }.toSet
  }

  def toCoordinates(gridCell: Cell): PointXYZ = {
    val x = gridCell._1 * cellDist
    val y = gridCell._2 * cellDist

    // NOTE:  negative z is higher.  Also need to shift z
    val z = -1 * (gridCell._3 << 4) * zCellDist
    origin.add(new PointXYZ(x, y, z))
  }

  def toCoordinates(x: Int, y: Int, z: Int): PointXYZ = toCoordinates((x, y, z, 0))

  def isEdgeXY(cell: Cell): Boolean = {
    val edge = Seq(0, sideLength - 1)
    edge.contains(cell._1) || edge.contains(cell._2)
  }

  def outwardHeadings(cell: Cell): Seq[Int] = {
    val rotations = mutable.Buffer[Int]()
    val (x, y) = (cell._1, cell._2)
    if(x == 0){
      rotations.append(Heading.W)
    }
    if(x == sideLength - 1){
      rotations.append(Heading.E)
    }
    if(y == 0){
      rotations.append(Heading.N)
    }
    if(y == sideLength - 1){
      rotations.append(Heading.S)
    }
    rotations
  }

  // return how many times you have to rotate CW to make the room's currentHeading point outwards
  def edgeRotationCount(cell: Cell, currentHeading: Int): Int = {
    require(isEdgeXY(cell))
    val rotations = outwardHeadings(cell)
    require(rotations.size > 0)
    GridManager.minRotationsToMatch(rotations, currentHeading)
    // if(rotations.contains(currentHeading)){
    //   0
    // }else if(rotations.contains(Heading.rotateCW(currentHeading))){
    //   1
    // }else if(rotations.contains(Heading.rotateCW(Heading.rotateCW(currentHeading)))){
    //   2
    // }else{
    //   3
    // }
  }


}
