package duchy.experiments.render.maze

import trn.RandomX
import trn.prefab.{BoundingBox, Heading}

import scala.collection.JavaConverters._


/**
  * Just a conversion of the old trn.maze.DfsMazeGen.Graph to scala, to get more code out of the java library.
  *
  * This is a strange way to represent a grid, though at least it uses sets to prevent more than one edge between nodes.
  *
  * A = node id, type, e.g. a pair of (X, Y)
  * B = the node type itself, e.g. BlockInfo
  */
class MutableMazeGraph[A, B] {

  val adjacencyList = scala.collection.mutable.Map[A, scala.collection.mutable.Set[A]]()

  def getNodes(): Iterable[A] = adjacencyList.keys

  def getAdjacencyList(): Map[A, scala.collection.mutable.Set[A]] = adjacencyList.toMap

  def getListFor(nodeId: A): scala.collection.mutable.Set[A] = {
    adjacencyList.getOrElseUpdate(nodeId, scala.collection.mutable.Set[A]())
  }

  def addEdge(node1: A, node2: A): Unit = {
    getListFor(node1).add(node2)
    getListFor(node2).add(node1)
  }

  def contains(nodeId: A): Boolean = adjacencyList.get(nodeId).map(list => list.nonEmpty).getOrElse(false)

  def containsEdge(node1: A, node2: A): Boolean = adjacencyList.get(node1).map { list =>
    list.contains(node2)
  }.getOrElse(false)

}

object MutableMazeGraph {

  type NodeId = Tuple2[Int, Int]

  def createGridMaze[B](width: Int, height: Int): MutableMazeGraph[NodeId, B] = {
    require(width >= 1 && height >= 1)
    val grid = new MutableMazeGraph[NodeId, B]()
    val start = (0, 0)
    createGridMaze(new RandomX(), grid, width, height, scala.collection.mutable.Set[NodeId](), start)
    grid
  }

  private def move(position: NodeId, vector: org.apache.commons.lang3.tuple.Pair[Integer, Integer]): NodeId = {
    val dx = vector.getLeft
    val dy = vector.getRight
    (position._1 + dx, position._2 + dy)
  }

  private def inBounds(xmin: Int, ymin: Int, width: Int, height: Int, position: NodeId): Boolean = {
    val x = position._1
    val y = position._2
    xmin <= x && x < xmin + width && ymin <= y && y < ymin + height
  }

  private def createGridMaze[B](
    random: RandomX,
    maze: MutableMazeGraph[NodeId, B],
    width: Int,
    height: Int,
    visitedList: scala.collection.mutable.Set[NodeId],
    currentNode: NodeId,
  ): Unit = if(!visitedList.contains(currentNode)){
    visitedList.add(currentNode)
    println(s"currentNode = ${currentNode}")

    val possibleMoves = Heading.all.asScala.map(h => move(currentNode, Heading.toUnitVector(h))).filter { node =>
      inBounds(0, 0, width, height, node) && !visitedList.contains(node)
    }
    if(possibleMoves.nonEmpty){
      val Branch = 3  // NOTE:  its not guaranteed to actually branch three times
      for(_ <- 0 until 3){
        val nextNode = random.randomElement(possibleMoves)
        if(! visitedList.contains(nextNode)){
          maze.addEdge(currentNode, nextNode)
          createGridMaze(random, maze, width, height, visitedList, nextNode);
        }
      }

    }
  }
}
