package trn.prefab.experiments.tiling

import trn.RandomX

import scala.collection.mutable


case class PlanNode(
    start: Boolean = false
)


object PlanEdge {
  def sorted(a: (Int, Int), b: (Int, Int)): PlanEdge = {
    val sq = Seq(a, b).sorted
    PlanEdge(sq(0), sq(1))
  }
}
case class PlanEdge(a: (Int, Int), b: (Int, Int))

/**
  * Basic structure of the map:  where the nodes and edges are.
  */
class TilePlan(val tiling: Tiling) {
  val nodes = mutable.Map[(Int, Int), PlanNode]()
  val edges = mutable.Set[PlanEdge]()

  def put(coord: (Int, Int), node: PlanNode): Unit = {
    nodes.put(coord, node)
  }

  def putEdge(edge: PlanEdge): Unit = {
    edges.add(edge)
  }

  def contains(coord: (Int, Int)): Boolean = nodes.contains(coord)

  def containsEdge(coordA: (Int, Int), coordB: (Int, Int)): Boolean = {
    val e = PlanEdge.sorted(coordA, coordB)
    edges.contains(e)
  }

  /**
    * Given a coordinate in col/row "tilespace" return a map of edge id -> TileEdge() objects telling us which
    * edges of the tile have a tile on the other side.
    *
    * @param tileCoord
    * @return
    */
  def getTileEdges(tileCoord: (Int, Int)): Map[Int, TileEdge] = {
    val coords: Iterable[(Int, Int)] = nodes.keys
    coords.filter(neighboor => containsEdge(tileCoord, neighboor)).map{ neighboor =>
      val edgeId = tiling.edge(tileCoord, neighboor).get
      TileEdge(edgeId, neighboor)
    }.map(edge => edge.edgeId -> edge).toMap
  }

  /**
    * throws an error if there are an problems
    * @return
    */
  def checkIntegrity: Unit = {

    // exactly one start node
    require(1 == nodes.values.count(_.start == true))
  }
}

/**
  * Creates the basic level plan:  which tiles connect to which, where the gates are, etc.
  *
  * See also `RandomWalkGenerator`
  */
object TilePlanner {

  def generate(random: RandomX, tiling: Tiling): TilePlan = {

    val plan = new TilePlan(tiling)
    // place a start node
    var current = (0, 0)
    plan.put(current, PlanNode(start=true))
    plan.checkIntegrity

    current = randomWalk(random, plan, current, 5)
    plan.checkIntegrity

    current = randomWalk(random, plan, current, 5)
    plan.checkIntegrity

    plan
  }

  def randomWalk(random: RandomX, plan: TilePlan, start: (Int, Int), stepCount: Int): (Int, Int) = {
    var current = start
    for (_ <- 0 until stepCount){
      val next = randomStep(random, plan, current).getOrElse(throw new Exception("ran out of tiles to walk"))
      plan.put(next, PlanNode())
      plan.putEdge(PlanEdge.sorted(current, next))
      current = next
    }
    current
  }

  def randomStep(random: RandomX, plan: TilePlan, current: (Int, Int)): Option[(Int, Int)] = {
    val available = plan.tiling.neighboors(current).filterNot(plan.contains)
    random.randomElementOpt(available)
  }

}
