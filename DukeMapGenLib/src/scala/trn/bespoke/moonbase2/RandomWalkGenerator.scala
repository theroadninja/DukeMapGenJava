package trn.bespoke.moonbase2

import trn.RandomX
import trn.logic.Point3d



object LogicalRoom {
  def apply(s: String): LogicalRoom = {
    val tag: Option[String] = s match {
      case "S" => Some("START")
      case "E" => Some("END")
      case s if s.endsWith("<") => Some("ONEWAY")
      case s if s.startsWith("K") => Some("KEY")
      case s if s.startsWith("G") => Some("GATE")
      case _ => None
    }
    val zone = s match {
      case "S" => 0
      case "E" => 3
      case s if s.startsWith("K") => s(1).toString.toInt - 1
      case s if s.startsWith("G") => s(1).toString.toInt - 1
      case s if s.endsWith("<") => s(0).toString.toInt
      case s => s.toInt
    }

    /** which key color, for keys and gates with locks.
      * Not the actual color, but the index of a color in a shuffled Seq of colors
      */
    val keyindex: Option[Int] = if(s.startsWith("K") || s.startsWith("G")){
      Some(s(1).toString.toInt - 1)
    }else{
      None
    }

    // TODO rename to higher zone or something
    val higherLevel: Option[Int] = if(s.endsWith("<")){
      Some(s(0).toString.toInt + 1)
    }else{
      None
    }

    LogicalRoom(s, zone, tag, keyindex, higherLevel)
  }

  /**
    * `p` is a point in the grid and `heading` says which side of p were are interested in.  It calculates
    * the "Side" spec of that side of p, namely whether it can have/needs to have a player-traversable connection
    * on that side, and the plot "zone" of the adjacent node, if there is one
    * @param p
    * @param heading which side of the tile at p to calculate
    * @param logicalMap
    * @tparam E
    * @return
    */
  def readSide[E](p: Point3d, heading: Int, logicalMap: LogicalMap[LogicalRoom, E]): Side = {
    val adjEdges = logicalMap.adjacentEdges(p)
    val adjNodes = logicalMap.adjacentNodes(p)
    val e = adjEdges.get(heading)
    val n = adjNodes.get(p + heading)

    val conn = if(n.isDefined){
      e.map(_ => TileSpec.ConnRequired).getOrElse(TileSpec.ConnBlocked)
    }else{
      // no node, can do whatever you want
      require(! e.isDefined)
      TileSpec.ConnOptional
    }
    Side(conn, n.map(_.zone))
  }
}

case class LogicalRoom(s: String, zone: Int, tag: Option[String], keyindex: Option[Int], higherZone: Option[Int]) {
  override def toString(): String = s
}

/**
  * Prototype that generates a "logical" map (a Graph) of locations and key/gate rooms.
  */
class RandomWalkGenerator(r: RandomX) {

  private def randomStep(current: Point3d, map: LogicalMap[LogicalRoom, String]): Option[Point3d] = {
    val available = map.emptyAdj(current).filter(_.z == current.z)
    if(available.isEmpty){
      throw new Exception("no empty spaces")
    }
    r.randomElementOpt(available)
  }

  private def randomWalk(start: Point3d, map: LogicalMap[LogicalRoom, String], value: String, stepCount: Int): Point3d = {
    var current = start
    for(_ <- 0 until stepCount){
      val nextOpt = randomStep(current, map) // TODO: DFS and backtrack if no options
      val next = nextOpt.getOrElse(throw new Exception("ran out of space; need to backtrack"))
      map.put(next, LogicalRoom(value))
      map.putEdge(current, next, "")
      current = next
    }
    current
  }

  private def attachGate(searchLevels: Seq[String], gate: String, map: LogicalMap[LogicalRoom, String]): Point3d = {
    val availableNodes = map.nodes.filter { case (p, v) =>
      searchLevels.contains(v.s) && map.emptyAdj(p).filter(_.z == p.z).nonEmpty
    }.keys
    require(availableNodes.size > 0)
    val current = r.randomElement(availableNodes)
    val gateLoc = randomStep(current, map).get
    map.put(gateLoc, LogicalRoom(gate))
    map.putEdge(current, gateLoc, "")
    gateLoc
  }

  private def addGateKey(at: String, gatekey: String, map: LogicalMap[LogicalRoom, String]): Point3d = {
    val p = r.randomElement(map.nodes.filter{case (p, v) => v.s == at })._1
    map.put(p, LogicalRoom(gatekey), overwrite = true)
    p
  }


  /** find all points in map, and all of their neighboors */
  def allPoints[N](map: LogicalMap[N, String]): Set[Point3d] = {
    map.nodes.keys.flatMap(p => p.adj).toSet
  }


  /** for testing */
  def hardcodedTest(): LogicalMap[LogicalRoom, String] = {
    val map = LogicalMap[LogicalRoom, String]()

    def putRow(map: LogicalMap[LogicalRoom, String], p: Point3d, rooms: Seq[String]): Unit = {
      (0 until rooms.size).zip(rooms).foreach { case(index, room) =>
        val p2 = p.copy(x = p.x + index)
        map.put(p2, LogicalRoom(room))
      }
    }

    putRow(map, Point3d(0, 0, 0), Seq("1",  "K2", "1<", "2"))
    putRow(map, Point3d(0, 1, 0), Seq("G1", "K1", "G2", "K3"))
    putRow(map, Point3d(0, 2, 0), Seq("E",  "0",  "0"))
    putRow(map, Point3d(0, 3, 0), Seq("3",  "G3", "S"))

    map.putEdge(Point3d(0, 0, 0), Point3d(1, 0, 0), "")
    map.putEdge(Point3d(1, 0, 0), Point3d(2, 0, 0), "1")
    map.putEdge(Point3d(2, 0, 0), Point3d(3, 0, 0), "2")

    (0 until 3).foreach(i => map.putEdge(Point3d(i, 1, 0), Point3d(i+1, 1, 0), "")) // horizontal edges, second row
    map.putEdge(Point3d(1, 2, 0), Point3d(2, 2, 0), "")
    map.putEdge(Point3d(0, 3, 0), Point3d(1, 3, 0), "")

    // vertical
    map.putEdge(Point3d(0, 0, 0), Point3d(0, 1, 0), "")
    map.putEdge(Point3d(3, 0, 0), Point3d(3, 1, 0), "")
    map.putEdge(Point3d(1, 1, 0), Point3d(1, 2, 0), "")
    (0 until 3).foreach(i => map.putEdge(Point3d(i, 2, 0), Point3d(i, 3, 0), ""))

    map
  }

  /** main generation code */
  def generate(): LogicalMap[LogicalRoom, String] = {
    val map = LogicalMap[LogicalRoom, String]()

    var current = Point3d(0, 0, 0)
    map.put(current, LogicalRoom("S"))
    randomWalk(current, map, "0", 3)

    // pick place to add gateway
    current = attachGate(Seq("0"), "G1", map)
    randomWalk(current, map, "1", 3)

    current = attachGate(Seq("1", "0"), "G2", map)
    randomWalk(current, map, "2", 3)

    current = attachGate(Seq("2", "1", "0"), "G3", map)
    val last = randomWalk(current, map, "3", 3)

    val end = randomStep(last, map).get
    map.put(end, LogicalRoom("E"))
    map.putEdge(last, end, "")

    addGateKey("0", "K1", map)
    addGateKey("1", "K2", map)
    addGateKey("2", "K3", map)

    // add a one-way node
    // TODO allow it to connect to gates and keys also
    def onewayNode(p: Point3d): Option[(Point3d, String)] = {
      val nodes = map.adjacentNodes(p).values.toSeq
      Seq(0, 1, 2).collectFirst{
        case i if nodes.count(_.s == i.toString) == 1 && nodes.count(_.s == (i+1).toString) == 1 =>
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
      map.put(p, LogicalRoom(s"${lower}<"))
      val higher = (1 + Integer.parseInt(lower)).toString

      val lowerNode = p.adj.collectFirst{case adj if map.nodes.get(adj).map(_.s).getOrElse("") == lower => adj}.get
      val higherNode = p.adj.collectFirst{case adj if map.nodes.get(adj).map(_.s).getOrElse("") == higher => adj}.get
      map.putEdge(p, lowerNode, lower)
      map.putEdge(p, higherNode, higher)
    }

    map
  }

}
