package trn.prefab.experiments.dijkdrop

import trn.{BuildConstants, UniqueTags, RandomX, HardcodedConfig, ScalaMapLoader, Sprite, Map => DMap}
import trn.prefab.{FallConnector, MapWriter, DukeConfig, GameConfig, SectorGroup, RedwallConnector, PrefabPalette, PastedSectorGroup, SpriteLogicException, Marker}
import trn.prefab.experiments.ExpUtil
import trn.prefab.experiments.dijkdrop.NodePalette.{getUnlinkedTunnelConnIds, Green, Red, Blue, White}

import scala.collection.mutable
import scala.collection.JavaConverters._


/**
  * everything needed to make the main area of a "node"
  * @param sg
  */
case class NodeTile2(sg: SectorGroup, tunnelTheme: Int) {

}

case class Node2(nodeId: String, tile: NodeTile2) {

  def tunnelTheme: Int = tile.tunnelTheme
}

case class Edge(startNode: String, endNode: String) {
  var startFallConnectorId: Option[Int] = None
  var endFallConnectorId: Option[Int] = None

  def readyToLink: Boolean = startFallConnectorId.isDefined && endFallConnectorId.isDefined
}

class MutableGraph {
  val nodes = mutable.Map[String, Node2]()
  val edges = mutable.ArrayBuffer[Edge]()
  val edgesByStart = mutable.Map[String, mutable.ArrayBuffer[Edge]]()
  val edgesByEnd = mutable.Map[String, mutable.ArrayBuffer[Edge]]()

  def addNode(node: Node2): Unit = nodes.put(node.nodeId, node)

  def addEdge(from: String, to: String): Unit = {
    val e = Edge(from, to)
    MutableGraph.addEdgeToMap(edgesByStart, e.startNode, e)
    MutableGraph.addEdgeToMap(edgesByEnd, e.endNode, e)
    edges.append(e)
  }

  def edgesFrom(from: String): Seq[Edge] = edgesByStart.get(from).getOrElse(Seq.empty)
  def edgesTo(to: String): Seq[Edge] = edgesByEnd.get(to).getOrElse(Seq.empty)
}

object MutableGraph {
  def addEdgeToMap[A, B](map: mutable.Map[A, mutable.ArrayBuffer[B]], key: A, edge: B): Unit = {
    if(!map.contains(key)){
      map.put(key, mutable.ArrayBuffer())
    }
    map(key).append(edge)
  }
}


// /** just a room with places to put tunnels */
// case class UnfinishedRoom(sg: SectorGroup, tunnelConnIds: Seq[Int]) {
// }
//
// object UnfinishedRoom {
//
//   def isTunnelConn(conn: RedwallConnector): Boolean = {
//     conn.getWallCount == 4 && conn.totalManhattanLength() == 2048 * 4
//   }
//
//
//   def apply(sg: SectorGroup): UnfinishedRoom = {
//     val copy = sg.copy()
//     val tunnelConns = copy.allUnlinkedRedwallConns.filter(isTunnelConn)
//     val ids = tunnelConns.map(_.getConnectorId).toSet
//     if(ids.size != tunnelConns.size){
//       throw new SpriteLogicException("redwall connectors for tunnel locations are not unique", tunnelConns.map(_.getLocationXY).asJava)
//     }
//     UnfinishedRoom(sg, ids.toSeq.sorted)
//   }
// }

object NodePalette {
  // THEMES (i.e. tunnel color)
  val Blue = 1
  val Red = 2
  val Green = 3
  val White = 4

  def isTunnelConn(conn: RedwallConnector): Boolean = {

    // TODO - pasting "loop" connectors is broken because the RedwallConnector.isMatch() fails
    // so for now I am hacking it by using 2 connectors per square.  One will be used for matching,
    // and the other will get matched based on its placement
    // conn.getWallCount == 4 && conn.totalManhattanLength() == 2048 * 4
    conn.getWallCount == 1 && conn.getConnectorId > 0 && conn.totalManhattanLength() == 2048
  }

  def getUnlinkedTunnelConns(sg: SectorGroup): Seq[RedwallConnector] = {
    sg.allUnlinkedRedwallConns.filter(isTunnelConn)
  }

  def getUnlinkedTunnelConnIds(sg: SectorGroup): Seq[Int] = {
    val tunnelConns = getUnlinkedTunnelConns(sg)
    val ids = tunnelConns.map(_.getConnectorId).toSet

    if (ids.size != tunnelConns.size) {
      throw new SpriteLogicException("redwall connectors for tunnel locations are not unique", tunnelConns.map(_.getLocationXY).asJava)
    }
    ids.toSeq.sorted
  }

}

class NodePalette(gameCfg: GameConfig, random: RandomX, palette: PrefabPalette) {


  val blueRoom = NodeTile2(palette.getSG(1), Blue)
  val redRoom = NodeTile2(palette.getSG(2), Red)
  val greenRoom = NodeTile2(palette.getSG(3), Green)
  val whiteRoom = NodeTile2(palette.getSG(4), White)


  // tunnel connector that goes nowhere
  val blank = palette.getSG(99)

  val outBlue = palette.getSG(100)
  val inBlue = palette.getSG(101)

  val outRed = palette.getSG(102)
  val inRed = palette.getSG(103)

  val outGreen = palette.getSG(104)
  val inGreen = palette.getSG(105)

  val outWhite = palette.getSG(106)
  val inWhite = palette.getSG(107)

  val floorTunnels: Map[Int, SectorGroup] = Map(
    Blue -> outBlue,
    Red -> outRed,
    Green -> outGreen,
    White -> outWhite,
  )
  val ceilingTunnels: Map[Int, SectorGroup] = Map(
    Blue -> inBlue,
    Red -> inRed,
    Green -> inGreen,
    White -> inWhite,
  )

  def isFallConn(sprite: Sprite): Boolean = Marker.isMarker(sprite, Marker.Lotags.FALL_CONNECTOR)

  def getFloorTunnel(connectorId: Int, tunnelTheme: Int): SectorGroup = {
    val tSG = floorTunnels(tunnelTheme)
    tSG.withModifiedSprites { sprite =>
      if(isFallConn(sprite)) {
        sprite.setHiTag(connectorId)
      }
    }
  }

  def getCeilingTunnel(connectorId: Int, tunnelTheme: Int): SectorGroup = {
    val tSG = ceilingTunnels(tunnelTheme)
    tSG.withModifiedSprites { sprite =>
      if(isFallConn(sprite)) {
        sprite.setHiTag(connectorId)
      }
    }
  }


  /**
    * this function needs to know about incoming and outgoing nodes
    * @param node
    * @return
    */
  def getSg(graph: MutableGraph, nodeId: String, node: Node2): SectorGroup = {

    // // TODO for the nodes, maybe its more than just theme (dont want to select based on theme)
    // var sg = node.tile.tunnelTheme match {
    //   case Blue => blueRoom.sg
    //   case Red => redRoom.sg
    // }
    var sg = node.tile.sg

    val connIds: Seq[Int] = random.shuffle(NodePalette.getUnlinkedTunnelConnIds(sg)).toSeq

    val outCount = graph.edgesFrom(nodeId).size
    val inCount = graph.edgesTo(nodeId).size
    if(connIds.size < outCount + inCount) {
      throw new SpriteLogicException(s"not enough spaces for tunnels")
    }

    // outgoing (floor)
    graph.edgesFrom(nodeId).zipWithIndex.foreach { case (edge, index) =>
      val conn = sg.getRedwallConnector(connIds(index))

      val fallConnectorId = 1000 + index
      edge.startFallConnectorId = Some(fallConnectorId)
      val destNode = graph.nodes(edge.endNode)
      val tunnelSg = getFloorTunnel(fallConnectorId, destNode.tunnelTheme)
      sg = sg.withGroupAttachedAutoRotate(gameCfg, conn, tunnelSg){ otherSg =>
        otherSg.allRedwallConnectors.find(_.getWallCount == 1).get
      }
    }

    // incoming (ceiling)
    graph.edgesTo(nodeId).zipWithIndex.foreach { case (edge, index) =>
      val conn = sg.getRedwallConnector(connIds(outCount + index))
      val fallConnectorId = 1000 + outCount + index
      val otherNode = graph.nodes(edge.startNode)
      val tunnelSg = getCeilingTunnel(fallConnectorId, otherNode.tunnelTheme)
      edge.endFallConnectorId = Some(fallConnectorId)
      sg = sg.withGroupAttachedAutoRotate(gameCfg, conn, tunnelSg){ otherSg =>
        otherSg.allRedwallConnectors.find(_.getWallCount == 1).get
      }
    }

    var break = false
    while(! break){
      val connOpt = NodePalette.getUnlinkedTunnelConns(sg).headOption
      if(connOpt.isDefined) {
        sg = sg.withGroupAttachedAutoRotate(gameCfg, connOpt.get, blank) { otherSg =>
          otherSg.allRedwallConnectors.find(_.getWallCount == 1).get
        }
      }else{
        break = true
      }
    }
    sg.autoLinked
  }
}

/**
  * Trying to create a c.s. graph and then alter the nodes to fit it.
  *
  * Less ambitious than my first attempt, but want to get something working cleaning so I can understand
  * whether it is necessary to model a node as having connection "points" on it, or if those become irrelevant
  * if you can alter the SectorGroups to match the node.
  */
object Dijkdrop2 {
  val Filename = "dijkdrp2.map"

  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    val input: DMap = ScalaMapLoader.loadMap(HardcodedConfig.EDUKE32PATH + Filename)
    val result = tryRun(gameCfg, input)
    ExpUtil.write(result)
  }

  def tryRun(gameCfg: GameConfig, input: DMap): DMap = {
    val writer = MapWriter(gameCfg)
    try {
      val random = RandomX()
      run(gameCfg, random, input, writer)
    } catch {
      case e: SpriteLogicException => {
        e.printStackTrace()
        ExpUtil.finish(writer, removeMarkers = false, errorOnWarnings = false)
        writer.outMap
      }
    }
  }

  def nextTag(gameCfg: GameConfig, map: DMap): Int = UniqueTags.nextTag(gameCfg, map)

  def run(gameCfg: GameConfig, random: RandomX, inputMap: DMap, writer: MapWriter): DMap = {
    val palette = ScalaMapLoader.paletteFromMap(gameCfg, inputMap)

    val nodepal = new NodePalette(gameCfg, random, palette)

    val graph = new MutableGraph()
    graph.addNode(Node2("1", nodepal.blueRoom))
    graph.addNode(Node2("2", nodepal.redRoom))
    graph.addNode(Node2("3", nodepal.greenRoom))
    graph.addNode(Node2("4", nodepal.whiteRoom))
    graph.addEdge("1", "2")
    graph.addEdge("2", "1")

    graph.addEdge("1", "3")
    graph.addEdge("3", "1")

    graph.addEdge("2", "3")

    graph.addEdge("2", "4")
    graph.addEdge("4", "3")




    var index = 0
    val layout = new GridLayout(BuildConstants.MapBounds, 6, 6)
    val pastedGroups = graph.nodes.map { case (nodeId, node) =>
      val sg = nodepal.getSg(graph, nodeId, node)
      val psg = writer.tryPasteInside(sg, layout.bbForIndex(index))
      index += 1
      nodeId -> psg.get
    }.toMap


    def getFallConnector(psg: PastedSectorGroup, connId: Int): FallConnector = {
      psg.allConnsInPsg.find(c => FallConnector.isFallConn(c) && c.getConnectorId == connId).get.asInstanceOf[FallConnector]
    }

    var channel = nextTag(gameCfg, writer.getMap)
    graph.edges.foreach { edge =>
      if(edge.readyToLink){
        val start = pastedGroups(edge.startNode)
        val startConn = getFallConnector(start, edge.startFallConnectorId.get)
        val end = pastedGroups(edge.endNode)
        val endConn = getFallConnector(end, edge.endFallConnectorId.get)
        startConn.linkConnectors(writer.getMap, endConn, channel)
        channel += 1
      }else{
        println(s"not ready: ${edge}")
      }
    }

    // val psg = writer.tryPasteInside(sg1, layout.bbForIndex(0)).get

    ExpUtil.finish(writer)
    writer.outMap
  }
}
