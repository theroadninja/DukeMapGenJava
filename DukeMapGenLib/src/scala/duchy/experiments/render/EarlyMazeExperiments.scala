package duchy.experiments.render

import duchy.experiments.render.maze.MutableMazeGraph
import trn.duke.experiments.E8StoneTunnels
import trn.maze.DfsMazeGen

import collection.JavaConverters._
import org.apache.commons.lang3.tuple.Pair
import MutableMazeGraph.NodeId
import trn.prefab.BoundingBox



/**
  * This is intended to be a rewrite of early experiments that created mazes, done in Java.
  */
object EarlyMazeExperiments {

  def getBoundingBoxInclusive(maze: MutableMazeGraph[NodeId, String]): BoundingBox = {
    // inclusive on both ends
    val (xs, ys) = maze.getNodes().unzip
    BoundingBox(
      xs.min,
      ys.min,
      xs.max,
      ys.max,
    )
  }

  def gridToStr(maze: MutableMazeGraph[NodeId, String]): String = {
    val bb = getBoundingBoxInclusive(maze)
    val str = scala.collection.mutable.ListBuffer[String]()
    for(y <- bb.yMax to bb.yMin by -1){
      for(x <- bb.xMin to bb.xMax){
        if(maze.contains((x, y))){
          str.append("#")
        }else{
          str.append(" ")
        }
        if(maze.containsEdge((x, y), (x+1, y))){
          str.append("-")
        }else{
          str.append(" ")
        }
      }
      str.append("\n")

      // in-between rows
      for(x <- bb.xMin to bb.xMax){
        if(maze.containsEdge((x, y), (x, y - 1))){
          str.append("| ")
        }else{
          str.append("  ")
        }
      }
      str.append("\n")
    }
    str.mkString("")
  }

  def main(args: Array[String]): Unit = {
    // E8StoneTunnels.main(args)
    // println("test")
    val maze = MutableMazeGraph.createGridMaze[String](10, 10)
    println(gridToStr(maze))
  }

}
