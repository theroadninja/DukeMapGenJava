package duchy.experiments.render.maze

import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.Pair


object OldHeadingScala {

  // val EAST = OldHeading.EAST
  // val SOUTH = OldHeading.SOUTH
  // val WEST = OldHeading.WEST
  // val NORTH = OldHeading.NORTH

  val NORTH = OldHeadingScala(0, -1, 0)
  val EAST = OldHeadingScala(1, 0, 1)
  val SOUTH = OldHeadingScala(0, 1, 2)
  val WEST = OldHeadingScala(-1, 0, 3)

  def dukeAngleOf(heading: OldHeadingScala): Int = heading match {
    case OldHeadingScala.NORTH => 1536
    case OldHeadingScala.EAST => 0
    case OldHeadingScala.SOUTH => 512
    case OldHeadingScala.WEST => 1024
    case _ => throw new IllegalArgumentException()

  }

}

case class OldHeadingScala(dx: Int, dy: Int, arrayIndex: Int){

  def move(node: Pair[Integer, Integer]): Pair[Integer, Integer] = {
    new ImmutablePair[Integer, Integer](node.getLeft + dx, node.getRight + dy)
  }
}
