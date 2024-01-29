package duchy.experiments.render.maze

import org.apache.commons.lang3.tuple.Pair

/** Legacy class */
trait Block {
  def getGridCoordinate: Pair[Integer, Integer]

  def getConnector(heading: OldHeadingScala): LegacyConnector

  def getEastConnector: LegacyConnector

  def getSouthConnector: LegacyConnector

  def draw(map: trn.Map): Int

}
