package trn.prefab.experiments.dijkdrop

import trn.{BuildConstants, UniqueTags, RandomX, HardcodedConfig, ScalaMapLoader, Sprite, Map => DMap}
import trn.prefab.{FallConnector, MapWriter, DukeConfig, EnemyMarker, SectorGroup, RedwallConnector, SpritePrefab, PrefabPalette, Item, PastedSectorGroup, Enemy, GameConfig, SpriteLogicException, Marker}
import trn.prefab.experiments.ExpUtil
import trn.prefab.experiments.dijkdrop.DropPalette2._
import trn.prefab.experiments.dijkdrop.NodePalette.getUnlinkedTunnelConnIds
import trn.prefab.experiments.dijkdrop.SpriteGroups.{STANDARD_AMMO, StandardAmmo, BASIC_AMMO, BasicAmmo}

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer




/**
  * everything needed to make the main area of a "node"
  * @param sg
  */
case class NodeTile2(sg: SectorGroup, tunnelTheme: Int, maxEdges: Int) {

  // TODO should this be allUnlinkedRedwallConnectors instead?
  val tunnelConnIds = sg.allRedwallConnectors.filter(NodePalette.isTunnelConn).map(_.getConnectorId)

  def modified(fn: SectorGroup => SectorGroup): NodeTile2 = NodeTile2(fn(sg))

  def withEnemies(random: RandomX, enemies: Seq[SpritePrefab], hitag: Int = 0): NodeTile2 = modified { sg =>
    Utils.withRandomSprites(sg, hitag, Marker.Lotags.ENEMY, random.shuffle(enemies).toSeq)
  }

  def withRandomItems(random: RandomX, items: Seq[SpritePrefab], hitag: Int = 0): NodeTile2 = modified { sg =>
    Utils.withRandomSprites(sg, hitag, Marker.Lotags.RANDOM_ITEM, random.shuffle(items).toSeq)
  }

}

object NodeTile2 {
  def apply(sg: SectorGroup, tunnelTheme: Int): NodeTile2 = {
    val maxEdges = sg.allRedwallConnectors.filter(NodePalette.isTunnelConn).size
    NodeTile2(sg, tunnelTheme, maxEdges)
  }

  def apply(sg: SectorGroup): NodeTile2 = {
    val theme = sg.allSprites.find(s => Marker.isMarker(s, Marker.Lotags.ALGO_GENERIC)).get.getHiTag
    apply(sg, theme)
  }
}

case class TunnelTile(sg: SectorGroup, tunnelTheme: Int, isFloor: Boolean) {

}

object TunnelTile {
  def apply(sg: SectorGroup): TunnelTile = {
    if(sg.allRedwallConnectors.size != 2){
      throw new SpriteLogicException("tunnel sector group has wrong number of redwall connectors")
    }
    val theme = sg.allSprites.find(s => Marker.isMarker(s, Marker.Lotags.ALGO_GENERIC)).get.getHiTag
    val isFloor = sg.allConnectors.find(c => FallConnector.isFallConn(c)).get.asInstanceOf[FallConnector].closerToFloor()
    TunnelTile(sg, theme, isFloor)
  }
}

case class NodeProperties(key: Boolean = false)

object NodeProperties {
  val Default = NodeProperties()
}

case class Node2(nodeId: String, tile: NodeTile2, props: NodeProperties = NodeProperties.Default) {

  def tunnelTheme: Int = tile.tunnelTheme

  def maxEdges: Int = tile.maxEdges // TODO does not account for special vs not
}


case class Edge(startNode: String, endNode: String) {
  // the id of the redwall connector that becomes the fall connector
  var startConnectorId: Option[Int] = None
  var startFallConnectorId: Option[Int] = None // assigned when sector group created

  var endFallConnectorId: Option[Int] = None

  def readyToLink: Boolean = startFallConnectorId.isDefined && endFallConnectorId.isDefined
}

class MutableGraph {
  val nodes = mutable.Map[String, Node2]()
  val edges = mutable.ArrayBuffer[Edge]()
  val edgesByStart = mutable.Map[String, mutable.ArrayBuffer[Edge]]()
  val edgesByEnd = mutable.Map[String, mutable.ArrayBuffer[Edge]]()

  def addNode(node: Node2): Unit = nodes.put(node.nodeId, node)

  def addEdge(from: String, to: String): Edge = {
    val e = Edge(from, to)
    MutableGraph.addEdgeToMap(edgesByStart, e.startNode, e)
    MutableGraph.addEdgeToMap(edgesByEnd, e.endNode, e)
    edges.append(e)
    e
  }

  def edgeExists(nodeA: String, nodeB: String): Boolean = edgesByStart.get(nodeA).map(list =>
    list.exists(_.endNode == nodeB)
  ).getOrElse(false)

  def edgesFrom(from: String): Seq[Edge] = edgesByStart.get(from).getOrElse(Seq.empty)
  def edgesTo(to: String): Seq[Edge] = edgesByEnd.get(to).getOrElse(Seq.empty)

  def edgeCount(nodeId: String): Int = edgesFrom(nodeId).size + edgesTo(nodeId).size
}

object MutableGraph {
  def addEdgeToMap[A, B](map: mutable.Map[A, mutable.ArrayBuffer[B]], key: A, edge: B): Unit = {
    if(!map.contains(key)){
      map.put(key, mutable.ArrayBuffer())
    }
    map(key).append(edge)
  }
}

object NodePalette {

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

  def standardRoomSetup(sg: SectorGroup): SectorGroup = {
    val sg2 = Utils.withRandomSprites(sg, STANDARD_AMMO, Marker.Lotags.RANDOM_ITEM, StandardAmmo)
    val sg3 = Utils.withRandomSprites(sg2, BASIC_AMMO, Marker.Lotags.RANDOM_ITEM, BasicAmmo)
    val sg4 = Utils.withRandomSprites(sg3, SpriteGroups.FOOT_SOLDIERS, Marker.Lotags.ENEMY, SpriteGroups.FootSoldiers)
    sg4
  }


}

class NodePalette(
  gameCfg: GameConfig,
  random: RandomX,
  palette: PrefabPalette,  // the main palette for this map
  stonePalette: PrefabPalette,  // <- a source map dedicated to the stone room
) {
}

case class SourceMapCollection(input: DMap, stoneInput: DMap)

/**
  * Trying to create a c.s. graph and then alter the nodes to fit it.
  *
  * Less ambitious than my first attempt, but want to get something working cleaning so I can understand
  * whether it is necessary to model a node as having connection "points" on it, or if those become irrelevant
  * if you can alter the SectorGroups to match the node.
  */
object Dijkdrop2 {
  val Filename = "dijkdrp2.map"
  val OtherFilename = "dijk/djkstone.map"

  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)

    // TODO map contents to a case class
    val input: DMap = ScalaMapLoader.loadMap(HardcodedConfig.EDUKE32PATH + Filename)
    val input2: DMap = ScalaMapLoader.loadMap(HardcodedConfig.EDUKE32PATH + OtherFilename)
    val result = tryRun(gameCfg, SourceMapCollection(input, input2))
    ExpUtil.write(result)
  }

  def tryRun(gameCfg: GameConfig, input: SourceMapCollection): DMap = {
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

  def connectRandomNodes(random: RandomX, graph: MutableGraph): Unit = {
    // val MaxEdges = 4

    val available = mutable.ArrayBuffer[String]()
    graph.nodes.keys.foreach { nodeId =>
      val cnt = graph.nodes(nodeId).maxEdges - graph.edgeCount(nodeId)
      for(_ <- 0 until cnt){
        available.append(nodeId)
      }
    }

    val (edges, _) = random.randomUnequalPairs(available)
    edges.foreach { case (a, b) =>
      if(!graph.edgeExists(a, b)){
        graph.addEdge(a, b)
      }
    }
  }

  /**
    * this function needs to know about incoming and outgoing nodes
    *
    * @param node
    * @return
    */
  def getSg(gameCfg: GameConfig, random: RandomX, nodepal: DropPalette2, graph: MutableGraph, nodeId: String, node: Node2): SectorGroup = {

    var sg = node.tile.sg
    if (node.props.key) {
      sg = sg.withItem2(Item.BlueKey)
    }


    val allConnIds: Seq[Int] = random.shuffle(NodePalette.getUnlinkedTunnelConnIds(sg)).toSeq
    val outCount = graph.edgesFrom(nodeId).size
    val inCount = graph.edgesTo(nodeId).size
    if (allConnIds.size < outCount + inCount) {
      throw new SpriteLogicException(s"not enough spaces for tunnels sgId=${sg.getGroupId} unlinked conns = ${allConnIds.size}")
    }
    val connIds = allConnIds.filter { i => i < 500 }

    // outoing, special/hardcoded
    graph.edgesFrom(nodeId).filter(_.startConnectorId.isDefined).zipWithIndex.foreach { case (edge, index) =>
      val conn = sg.getRedwallConnector(edge.startConnectorId.get)
      val fallConnectorId = 2000 + index
      edge.startFallConnectorId = Some(fallConnectorId)
      val destNode = graph.nodes(edge.endNode)
      val tunnelSg = nodepal.getFloorTunnel(fallConnectorId, destNode.tunnelTheme)
      sg = sg.withGroupAttachedAutoRotate(gameCfg, conn, tunnelSg) { otherSg =>
        otherSg.allRedwallConnectors.find(_.getWallCount == 1).get
      }
    }

    // outgoing (floor)
    graph.edgesFrom(nodeId).filter(_.startConnectorId.isEmpty).zipWithIndex.foreach { case (edge, index) =>
      val conn = sg.getRedwallConnector(connIds(index))

      val fallConnectorId = 1000 + index
      edge.startFallConnectorId = Some(fallConnectorId)
      val destNode = graph.nodes(edge.endNode)
      val tunnelSg = nodepal.getFloorTunnel(fallConnectorId, destNode.tunnelTheme)
      sg = sg.withGroupAttachedAutoRotate(gameCfg, conn, tunnelSg) { otherSg =>
        otherSg.allRedwallConnectors.find(_.getWallCount == 1).get
      }
    }

    // incoming (ceiling)
    graph.edgesTo(nodeId).zipWithIndex.foreach { case (edge, index) =>
      val conn = sg.getRedwallConnector(connIds(outCount + index))
      val fallConnectorId = 1000 + outCount + index
      val otherNode = graph.nodes(edge.startNode)
      val tunnelSg = nodepal.getCeilingTunnel(fallConnectorId, otherNode.tunnelTheme)
      edge.endFallConnectorId = Some(fallConnectorId)
      sg = sg.withGroupAttachedAutoRotate(gameCfg, conn, tunnelSg) { otherSg =>
        otherSg.allRedwallConnectors.find(_.getWallCount == 1).get
      }
    }

    var break = false
    while (!break) {
      val connOpt = NodePalette.getUnlinkedTunnelConns(sg).headOption
      if (connOpt.isDefined) {
        sg = sg.withGroupAttachedAutoRotate(gameCfg, connOpt.get, nodepal.blank) { otherSg =>
          otherSg.allRedwallConnectors.find(_.getWallCount == 1).get
        }
      } else {
        break = true
      }
    }
    sg.autoLinked
  }

  /** for testing */
  def makeGraph(random: RandomX, nodepal: DropPalette2): MutableGraph = {

    val startRooms = Seq(nodepal.startRoom)
    val gateRooms = Seq(nodepal.redGate)
    val exits = Seq(nodepal.exitRoom)
    val keyRooms = Seq(nodepal.blueItemRoom)

    val normalRooms = random.shuffle(Seq(
      // nodepal.blueRoom, nodepal.redRoom, nodepal.greenRoom, nodepal.whiteRoom, nodepal.dirtRoom, nodepal.woodRoom, nodepal.grayRoom
      // nodepal.bluePentagon,
      nodepal.buildingEdge, nodepal.cavern, nodepal.nukeSymbolCarpet,
      nodepal.castleStairs, nodepal.greenCastle, nodepal.moon3way,
      nodepal.bathrooms, nodepal.parkingGarage, nodepal.stoneVaults, nodepal.fountain
    )).toSeq


    val graph = new MutableGraph()
    graph.addNode(Node2("START", random.randomElement(startRooms)))
    graph.addNode(Node2("GATE", random.randomElement(gateRooms)))
    graph.addNode(Node2("EXIT", random.randomElement(exits)))
    graph.addNode(Node2("KEY", random.randomElement(keyRooms), NodeProperties(key=true)))

    graph.addNode(Node2("1", normalRooms(0)))
    graph.addNode(Node2("3", normalRooms(1)))
    graph.addNode(Node2("4", normalRooms(2)))
    graph.addNode(Node2("5", normalRooms(3)))
    graph.addNode(Node2("6", normalRooms(4)))
    graph.addNode(Node2("8", normalRooms(5)))

    val edge = graph.addEdge("GATE", "EXIT")
    edge.startConnectorId = Some(999)

    graph.addEdge("START", "1")
    graph.addEdge("1", "GATE")
    graph.addEdge("GATE", "3")
    graph.addEdge("3", "4")
    graph.addEdge("4", "5")
    graph.addEdge("5", "6")
    graph.addEdge("6", "KEY")
    graph.addEdge("KEY", "8")
    graph.addEdge("8", "1")

    connectRandomNodes(random, graph)
    graph
  }

  def run(gameCfg: GameConfig, random: RandomX, input: SourceMapCollection, writer: MapWriter): DMap = {
    val palette = ScalaMapLoader.paletteFromMap(gameCfg, input.input)
    val stonePalette = ScalaMapLoader.paletteFromMap(gameCfg, input.stoneInput)

    // val nodepal = new NodePalette(gameCfg, random, palette, stonePalette)
    val nodepal = new DropPalette2(gameCfg, random, palette, stonePalette)

    val graph = makeGraph(random, nodepal)
    println(s"start edge count ${graph.edgeCount("START")}")

    var index = 0
    val layout = new GridLayout(BuildConstants.MapBounds, 5, 5)
    val pastedGroups = graph.nodes.map { case (nodeId, node) =>
      val sg = getSg(gameCfg, random, nodepal, graph, nodeId, node)
      val bb = layout.bbForIndex(index)
      val psg = writer.tryPasteInside(sg, bb).getOrElse(throw new SpriteLogicException(s"sector group too big for ${bb.width} X ${bb.height}"))
      index += 1
      nodeId -> psg
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
