package duchy.experiments.render.maze

import org.apache.commons.lang3.tuple.Pair

import java.util

/**
  * This legacy code is best understood while listening to the Tron Legacy soundtrack.
  */
class Grid {

  private val gridData: java.util.Map[Pair[Integer, Integer], Block] = new util.HashMap[Pair[Integer, Integer], Block]

  def add(block: Block): Unit = {
    gridData.put(block.getGridCoordinate, block)
  }

  def getBlock(node: Pair[Integer, Integer]): Block = {
    get(node)
  }

  def get(node: Pair[Integer, Integer]): Block = {
    gridData.get(node)
  }

  def getNodes: java.util.Set[Pair[Integer, Integer]] = {
    gridData.keySet
  }

  def contains(node: Pair[Integer, Integer]): Boolean = {
    gridData.containsKey(node)
  }

}
