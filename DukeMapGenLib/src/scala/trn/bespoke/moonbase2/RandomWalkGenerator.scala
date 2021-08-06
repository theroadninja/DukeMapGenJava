package trn.bespoke.moonbase2

import trn.logic.Point3d
import trn.prefab.RandomX

/**
  * Prototype that generates a "logical" map (a Graph) of locations and key/gate rooms.
  */
class RandomWalkGenerator(r: RandomX) {

  private def randomStep(current: Point3d, map: LogicalMap[String, String]): Option[Point3d] = {
    val available = map.emptyAdj(current).filter(_.z == current.z)
    if(available.isEmpty){
      throw new Exception("no empty spaces")
    }
    r.randomElementOpt(available)
  }

  private def randomWalk(start: Point3d, map: LogicalMap[String, String], value: String, stepCount: Int): Point3d = {
    var current = start
    for(_ <- 0 until stepCount){
      val nextOpt = randomStep(current, map) // TODO: DFS and backtrack if no options
      val next = nextOpt.getOrElse(throw new Exception("ran out of space; need to backtrack"))
      map.put(next, value)
      map.putEdge(current, next, "")
      current = next
    }
    current
  }

  private def attachGate(searchLevels: Seq[String], gate: String, map: LogicalMap[String, String]): Point3d = {
    val availableNodes = map.nodes.filter { case (p, v) =>
      searchLevels.contains(v) && map.emptyAdj(p).filter(_.z == p.z).nonEmpty
    }.keys
    val current = r.randomElement(availableNodes)
    val gateLoc = randomStep(current, map).get
    map.put(gateLoc, gate)
    map.putEdge(current, gateLoc, "")
    gateLoc
  }

  private def addGateKey(at: String, gatekey: String, map: LogicalMap[String, String]): Point3d = {
    val p = r.randomElement(map.nodes.filter{case (p, v) => v == at })._1
    map.put(p, gatekey, overwrite = true)
    p
  }


  /** find all points in map, and all of their neighboors */
  def allPoints(map: LogicalMap[String, String]): Set[Point3d] = {
    map.nodes.keys.flatMap(p => p.adj).toSet
  }

  def generate(): LogicalMap[String, String] = {
    val map = LogicalMap[String, String]()

    var current = Point3d(0, 0, 0)
    map.put(current, "S")
    randomWalk(current, map, "0", 3)

    // pick place to add gateway
    current = attachGate(Seq("0"), "G1", map)
    randomWalk(current, map, "1", 3)

    current = attachGate(Seq("1", "0"), "G2", map)
    randomWalk(current, map, "2", 3)

    current = attachGate(Seq("2", "1", "0"), "G3", map)
    val last = randomWalk(current, map, "3", 3)

    val end = randomStep(last, map).get
    map.put(end, "E")
    map.putEdge(last, end, "")

    addGateKey("0", "K1", map)
    addGateKey("1", "K2", map)
    addGateKey("2", "K3", map)

    // add a one-way node
    // TODO allow it to connect to gates and keys also
    def onewayNode(p: Point3d): Option[(Point3d, String)] = {
      val nodes = map.adjacentNodes(p).values.toSeq
      Seq(0, 1, 2).collectFirst{
        case i if nodes.count(_ == i.toString) == 1 && nodes.count(_ == (i+1).toString) == 1 =>
          (p, i.toString)
      }
    }
    // val oneway = map.nodes.keys.flatMap(p => map.emptyAdj(p)).find{ p =>
    //   val nodes = map.adjacentNodes(p).values.toSeq
    //   Seq(0, 1, 2).exists { i =>
    //     val j = i + 1
    //     nodes.count(_ == i.toString) == 1 && nodes.count(_ == j.toString) == 1
    //   }
    // }
    //val oneway = map.nodes.keys.flatMap(p => map.adjacentNodes(p)).collectFirst { case p if onewayNode(p).isDefined => onewayNode(p).get }
    val oneway = allPoints(map).filterNot(map.contains).collectFirst{
      case p if onewayNode(p).isDefined => onewayNode(p).get
    }
    oneway.foreach { case (p, lower) =>
      map.put(p, s"${lower}<")
      val higher = (1 + Integer.parseInt(lower)).toString

      val lowerNode = p.adj.collectFirst{case adj if map.nodes.get(adj).getOrElse("") == lower => adj}.get
      val higherNode = p.adj.collectFirst{case adj if map.nodes.get(adj).getOrElse("") == higher => adj}.get
      map.putEdge(p, lowerNode, lower)
      map.putEdge(p, higherNode, higher)
    }

    map
  }

}
