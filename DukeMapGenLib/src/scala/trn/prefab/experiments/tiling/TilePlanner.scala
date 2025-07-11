package trn.prefab.experiments.tiling

import trn.RandomX

import scala.collection.mutable


case class PlanNode(
  start: Boolean = false,
  end: Boolean = false,
  gate: Boolean = false,
  backdrop: Boolean = false, // area you can't traverse, is only there for decoration
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


  def get(coord: (Int, Int)): Option[PlanNode] = nodes.get(coord)
  def put(coord: (Int, Int), node: PlanNode): Unit = {
    nodes.put(coord, node)
  }

  def putEdge(edge: PlanEdge): Unit = {
    edges.add(edge)
  }
  def putEdge(coordA: (Int, Int), coordB: (Int, Int)): Unit = edges.add(PlanEdge.sorted(coordA, coordB))

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

    // get the neighboors of the given coordinate
    val neighboorCoords = coords.filter(neighboor => containsEdge(tileCoord, neighboor))

    val edges = neighboorCoords.map{ neighboor =>
      tiling.edge(tileCoord, neighboor).map(edgeId => TileEdge(edgeId, neighboor))
    }.collect { case Some(edge) => edge}

    edges.map(edge => edge.edgeId -> edge).toMap



    //neighboorCoords.map{ neighboor =>
    //  val edgeId = tiling.edge(tileCoord, neighboor).get
    //  TileEdge(edgeId, neighboor)
    //}.map(edge => edge.edgeId -> edge).toMap
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

  def fromHardcoded(tiling: Tiling, coords: Seq[(Int, Int)]): TilePlan = {

    val plan = new TilePlan(tiling)
    coords.foreach{ n => plan.put(n, PlanNode())}


    /*
    def allEdges(tileCoord: (Int, Int), coords: Seq[(Int, Int)]) = {
      coords.flatMap { neighboor =>
        val edgeIdOpt = tiling.edge(tileCoord, neighboor)
        edgeIdOpt.map(edgeId => TileEdge(edgeId, neighboor, None))
      }.map(edge => edge.edgeId -> edge).toMap
    }

     */
    coords.foreach { c1 =>
      coords.foreach{ c2 =>
        tiling.edge(c1, c2).foreach { _ =>
          plan.putEdge(c1, c2)

        }

      }
    }

    plan
  }

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

    val end = randomStep(random, plan, current).get
    plan.put(end, PlanNode(end=true))
    plan.putEdge(current, end)

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
