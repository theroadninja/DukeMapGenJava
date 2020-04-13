package trn.prefab.experiments

object GridUtil {

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

}
