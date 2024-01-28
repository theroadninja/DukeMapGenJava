package duchy.experiments.render.maze

import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.Pair


class BlockCursor(var x: Int, var y: Int) {

  def get: Pair[Integer, Integer] = new ImmutablePair(x, y)

  /**
    * Moves the turtle in direction of decreasing y and returns the NEW position
    *
    * @return the position after moving, with y -= 1
    */
  def moveNorth: Pair[Integer, Integer] = {
    this.y -= 1
    get
  }

  def moveEast: Pair[Integer, Integer] = {
    this.x += 1
    get
  }

}
