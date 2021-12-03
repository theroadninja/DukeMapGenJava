package trn.prefab.experiments

import trn.prefab.Heading

import scala.collection.mutable

object GridUtil {

  /**
    * Calculates all possible integer grid coordinates for the given dimensions
    * @param xcount how many x coordinate values there are (e.g. xcount=3 gives x coordinates: 0, 1, 2)
    * @param ycount
    * @param zcount
    * @param wcount
    * @return Seq of coordinates (each coordinate is a 4-tuple of (x, y, z, w))
    */
  def all4dGridCoordinates(xcount: Int, ycount: Int, zcount: Int, wcount: Int): Seq[(Int, Int, Int, Int)] = {
    require(xcount > 0 && ycount > 0 && zcount > 0 && wcount > 0)
    val results = mutable.ArrayBuffer[(Int, Int, Int, Int)]()
    for(x <- 0 until xcount; y <- 0 until ycount; z <- 0 until zcount; w <- 0 until wcount){
      results.append((x, y, z, w))
    }
    results
  }

  /**
    * Return all the direct neighboors (no diagonals). Works with any number of dimensions.
    * (1, 2) =>  (0, 2), (2, 2), (1, 1), (1, 3)
    * @param coord
    * @return all nodes directly adject (not including diagonals)
    */
  def adj(coord: Int *): Seq[Seq[Int]] = {
    (0 until coord.size).flatMap{ dimension =>
      Seq(
        coord.updated(dimension, coord(dimension) - 1),
        coord.updated(dimension, coord(dimension) + 1),
      )
    }
  }

  def neighboors(gridX: Int, gridY: Int): Seq[(Int,Int)] = adj(gridX, gridY).map(list => (list(0), list(1)))
  def neighboors(loc: (Int, Int)): Seq[(Int, Int)] = adj(loc._1, loc._2).map(list => (list(0), list(1)))
  def neighboors(cell: Cell2D): Seq[Cell2D] = adj(cell.x, cell.y).map(loc => Cell2D(loc(0), loc(1)))

  def isAdj(left: Seq[Int], right: Seq[Int]): Boolean = {
    require(left.size > 0 && left.size == right.size)
    (0 until left.size).map(i => Math.abs(left(i) - right(i))).sum == 1
  }

  def isAdj(left: Cell2D, right: Cell2D): Boolean = isAdj(left.toSeq, right.toSeq)

  /**
    * Given two grid cells, return the Heading to go from start to end.  Only works with cells
    * that are on an axis.
    * @param startX
    * @param startY
    * @param endX
    * @param endY
    * @return
    */
  def heading(startX: Int, startY: Int, endX: Int, endY: Int): Option[Int] = {
    // val xaxis = (startX == endX)
    // val yaxis = (startY == endY)
    (startX == endX, startY == endY) match {
      case (true, true) => None
      case (false, true) => if(startX < endX){ Some(Heading.E) }else{ Some(Heading.W) }
      case (true, false) => if(startY < endY){ Some(Heading.S) }else{ Some(Heading.N) }
      case _ => None
    }
  }

  def heading(start: Cell2D, end: Cell2D): Option[Int] = heading(start.x, start.y, end.x, end.y)

}
