package duchy.experiments.render

import duchy.experiments.render.maze.{BlockInfo, BlockTileset, MutableMazeGraph}
import trn.maze.DfsMazeGen

import collection.JavaConverters._
import org.apache.commons.lang3.tuple.Pair
import MutableMazeGraph.NodeId
import trn.{PlayerStart, RandomX, Sector, Map => DMap}
import trn.prefab.BoundingBox

import scala.+:



/**
  * This is intended to be a rewrite of early experiments that created mazes, done in Java.
  */
object EarlyMazeExperiments {

  // def getBoundingBoxInclusive(maze: MutableMazeGraph[NodeId, String]): BoundingBox = {
  def getBoundingBoxInclusive(nodes: Iterable[NodeId]): BoundingBox = {
    // inclusive on both ends
    val (xs, ys) = nodes.unzip
    BoundingBox(
      xs.min,
      ys.min,
      xs.max,
      ys.max,
    )
  }

  def gridToStr(maze: MutableMazeGraph[NodeId, String]): String = {
    val bb = getBoundingBoxInclusive(maze.getNodes())
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

  def gridToStr2(nodes: Set[NodeId]): String = {
    val bb = getBoundingBoxInclusive(nodes)
    val str = scala.collection.mutable.ListBuffer[String]()
    for (y <- bb.yMax to bb.yMin by -1) {
      for (x <- bb.xMin to bb.xMax) {
        if(nodes.contains((x, y))){
          str.append("#")
        }else{
          str.append(" ")
        }
      }
      str.append("\n")
    }
    str.mkString("")
  }

  def expand[B](maze: MutableMazeGraph[NodeId, B]): Set[NodeId] = {
    maze.getNodes().map { case (x, y) =>
      val newNode = (x * 2, y * 2)
      val fillerNodes = maze.getListFor((x, y)).map { case (neighboorX, neighboorY) =>
        (x + neighboorX, y + neighboorY)
      }
      fillerNodes.add(newNode)
      // fillerNodes.toSet
      fillerNodes
    }.flatten.toSet

  }

  def main(args: Array[String]): Unit = {
    // E8StoneTunnels.main(args)
    // println("test")

    val maze = MutableMazeGraph.createGridMaze[String](10, 10)
    println(gridToStr(maze))

    val maze2 = expand(maze)
    println(gridToStr2(maze2))
    //experiment6()
  }


  /**
    * From E6CreateMazeWBlocks.java
    */
  def experiment6(): Unit = {
    val graph = MutableMazeGraph.createGridMaze[String](10, 10)
    val random = RandomX()

    val MazeWallTex = 772
    val block0 = BlockTileset(MazeWallTex, 0, 0)
    val block1 = BlockTileset(781, 782, 781)
    val block2 = BlockTileset(800, 801, 800)
    val blocks = Seq(block0, block1, block2)
    val Z_STEP_HEIGHT = 1024
    val oneLevelDown = Sector.DEFAULT_FLOOR_Z + Z_STEP_HEIGHT * 6;
    val floorHeights = Seq(Sector.DEFAULT_FLOOR_Z, Sector.DEFAULT_FLOOR_Z, oneLevelDown)

    val nodeIdToBlock = graph.getNodes().map { nodeId =>
      val block = random.randomElement(blocks)
      val floorz = random.randomElement(floorHeights)
      nodeId -> BlockInfo(floorz, block)
    }.toMap

  }

  def createMap(): Unit = {
    val map = DMap.createNew()
    map.setPlayerStart(new PlayerStart(512, 512, 0, PlayerStart.SOUTH))

    val nodeIdToSector = scala.collection.mutable.Map[NodeId, Sector]()
  }

}
