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
    * @param hitag
    * @return
    */
  def apply(hitag: Int): AxisLock = AxisLock(hitag >>> 4, hitag % 16)

  def matchAll(axisLocks: Iterable[AxisLock], coords: Int*): Boolean = {
    require(coords.size > 0)
    !axisLocks.exists(ax => !ax.matches(coords: _*))
  }

}