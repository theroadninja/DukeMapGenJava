package duchy.experiments.render.maze
import org.apache.commons.lang3.tuple.Pair
import trn.PointXY
import trn.maze.Heading

abstract class AbstractBlock(val gridCoordinate: Pair[Integer, Integer]) extends Block {
  val WALL_LENGTH: Int = 2048 //2 x largest grid size

  override def getGridCoordinate: Pair[Integer, Integer] = {
    this.gridCoordinate
  }

  def getOuterWallLength: Int = {
    WALL_LENGTH
  }

  /** i think this is the physical x coordinate of the left edge in Build coordinates */
  def getWestEdge: Int = gridCoordinate.getLeft * WALL_LENGTH

  def getEastEdge: Int = (gridCoordinate.getLeft + 1) * WALL_LENGTH

  def getNorthEdge: Int = gridCoordinate.getRight * WALL_LENGTH

  def getSouthEdge: Int = (gridCoordinate.getRight + 1) * WALL_LENGTH

  def getCenter: PointXY = new PointXY((getWestEdge + getEastEdge) / 2, (getNorthEdge + getSouthEdge) / 2)

  override def getEastConnector: LegacyConnector = getConnector(Heading.EAST)

  override def getSouthConnector: LegacyConnector = getConnector(Heading.SOUTH)

}
