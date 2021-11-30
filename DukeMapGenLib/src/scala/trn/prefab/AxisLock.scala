package trn.prefab

/**
  * Describes the limiting of a room to a certain part of the grid.
  * For example, AxisLock(1, 6) locks it to y=6 on the grid (cant have any other y value)
  *
  * Supports any number of spatial dimensions from 1 to 4 (though its design leaves room for 8...)
  *
  * @param axis which axis:  0 for x, 1 for y, 2 for z, 3 for w
  * @param value the value of the axis to lock to
  *
  * Example:  AxisLock(1, 6) locks it to y=6 on the grid
  */
case class AxisLock(axis: Int, value: Int) {
  require(0 <= axis && axis <= 3)

  def matches(coords: Int*): Boolean = {
    require(axis < coords.size)
    value == coords(axis)
  }

}

object AxisLock {
  val X = 0
  val Y = 1
  val Z = 2
  val W = 3

  /**
    * Creates an AxisLock based on the Hitag value of "ALGO_AXIS_LOCK" marker sprites.
    *
    * If there is more than one sprite for a given axis, though, it should be an OR relationship
    * (because "AND" is impossible)
    *
    * @param hitag
    * @return
    */
  def apply(hitag: Int): AxisLock = AxisLock(hitag >>> 4, hitag % 16)

  def matchAll(axisLocks: Iterable[AxisLock], coords: Int*): Boolean = {
    require(coords.size > 0)

    // simple version, if we dont care about the OR relationship:
    // !axisLocks.exists(ax => !ax.matches(coords: _*))

    val axes = axisLocks.map(_.axis).toSet
    axes.map { axis =>
      // this should be true if one of the locks for the given axis matches:
      axisLocks.filter(_.axis == axis).exists(_.matches(coords: _*))
    }.find(_ == false).isEmpty
  }

}