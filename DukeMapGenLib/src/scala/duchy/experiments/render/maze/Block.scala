package duchy.experiments.render.maze

import org.apache.commons.lang3.tuple.Pair
import trn.maze.Heading

/** Legacy class */
trait Block {
  def getGridCoordinate: Pair[Integer, Integer]

  def getConnector(heading: Heading): LegacyConnector

  def getEastConnector: LegacyConnector

  def getSouthConnector: LegacyConnector

  def draw(map: trn.Map): Int

}
