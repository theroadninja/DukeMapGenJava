package trn.prefab.experiments.dijkdrop

import trn.{BuildConstants, UniqueTags, RandomX, HardcodedConfig, ScalaMapLoader, Sprite, Map => DMap}
import trn.prefab.{FallConnector, MapWriter, DukeConfig, EnemyMarker, SectorGroup, RedwallConnector, PrefabPalette, Item, PastedSectorGroup, Enemy, GameConfig, SpriteLogicException, Marker}
import trn.prefab.experiments.ExpUtil
import trn.prefab.experiments.dijkdrop.NodePalette.{getUnlinkedTunnelConnIds, Dirt, Gray, Green, Wood, Red, Blue, White}

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer




/**
  * everything needed to make the main area of a "node"
  * @param sg
  */
case class NodeTile2(sg: SectorGroup, tunnelTheme: Int, maxEdges: Int) {

  def modified(fn: SectorGroup => SectorGroup): NodeTile2 = NodeTile2(fn(sg))

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

  def maxEdges: Int = tile.maxEdges
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
  // THEMES (i.e. tunnel color)
  val Blue = 1
  val Red = 2
  val Green = 3
  val White = 4 // moon
  val Gray = 5 // like the gray brick texture
  val Dirt = 6 // classic dirt/canyon walls
  val Wood = 7 // wood

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
  val grayRoom = NodeTile2(palette.getSG(5), Gray)
  val dirtRoom = NodeTile2(palette.getSG(6), Dirt)
  val woodRoom = NodeTile2(palette.getSG(7), Wood)

  val bluePentagon = NodeTile2(palette.getSG(8))
  val blueItemRoom = NodeTile2(palette.getSG(9))
  val redGate = NodeTile2(palette.getSG(10))
  val exitRoom = NodeTile2(palette.getSG(11))

  val startRoom = NodeTile2(palette.getSG(12))
  val castleStairs = NodeTile2(palette.getSG(13)).modified { sg =>
    val enemies = Seq(
      Enemy.LizTroop, Enemy.LizTroop, Enemy.LizTroop,
      Enemy.LizTroopCmdr,
      Enemy.Enforcer,
      Enemy.OctaBrain,
      Enemy.Blank, Enemy.Blank, Enemy.Blank, Enemy.Blank,
    )
    val sg2 = Utils.withRandomSprites(sg, Marker.Lotags.RANDOM_ITEM, Seq(Item.SmallHealth, Item.MediumHealth, Item.MediumHealth, Item.ShotgunAmmo))
    Utils.withRandomSprites(sg2, Marker.Lotags.ENEMY, enemies)
  }

  val moon3way = NodeTile2(palette.getSG(14)).modified { sg =>
    val enemies = Seq(Enemy.LizTroop, Enemy.Enforcer, Enemy.Enforcer, Enemy.OctaBrain, Enemy.Blank, Enemy.AssaultCmdr)
    Utils.withRandomEnemies(sg, enemies)
  }


  def randomizeStartRoom(): NodeTile2 = {
    val startItems: Seq[Item] = random.shuffle(Seq(Item.Armor, Item.Medkit, Item.Shotgun, Item.Chaingun, Item.PipeBomb, Item.HandgunAmmo, Item.ChaingunAmmo)).toSeq
    startRoom.modified { sg =>
      Utils.withRandomSprites(sg, Marker.Lotags.RANDOM_ITEM, startItems)
      // val itemSlots: Int = sg.allSprites.filter(s => Marker.isMarker(s, Marker.Lotags.RANDOM_ITEM)).size
      // (0 until itemSlots).map(startItems).foldLeft(sg){ case (sg2, item) => sg2.withItem2(item)}
    }
  }


  // tunnel connector that goes nowhere
  val blank = palette.getSG(99)

  val tunnels = Seq(100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113).map(palette.getSG).map(sg => TunnelTile(sg))

  val floorTunnels = tunnels.filter(_.isFloor).map(tile => tile.tunnelTheme -> tile).toMap
  val ceilingTunnels = tunnels.filter(!_.isFloor).map(tile => tile.tunnelTheme -> tile).toMap

  def isFallConn(sprite: Sprite): Boolean = Marker.isMarker(sprite, Marker.Lotags.FALL_CONNECTOR)

  def getFloorTunnel(connectorId: Int, tunnelTheme: Int): SectorGroup = {
    val tSG = floorTunnels(tunnelTheme).sg
    tSG.withModifiedSprites { sprite =>
      if(isFallConn(sprite)) {
        sprite.setHiTag(connectorId)
      }
    }
  }

  def getCeilingTunnel(connectorId: Int, tunnelTheme: Int): SectorGroup = {
    val tSG = ceilingTunnels(tunnelTheme).sg
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
    if(node.props.key){
      sg = sg.withItem2(Item.BlueKey)
    }

    val connIds: Seq[Int] = random.shuffle(NodePalette.getUnlinkedTunnelConnIds(sg)).toSeq.filter { i =>
      i < 500 // filter out the special/hardcoded connectors
    }

    val outCount = graph.edgesFrom(nodeId).size
    val inCount = graph.edgesTo(nodeId).size
    if(connIds.size < outCount + inCount) {
      throw new SpriteLogicException(s"not enough spaces for tunnels")
    }

    // outoing, special/hardcoded
    graph.edgesFrom(nodeId).filter(_.startConnectorId.isDefined).zipWithIndex.foreach { case (edge, index) =>
      val conn = sg.getRedwallConnector(edge.startConnectorId.get)
      val fallConnectorId = 2000 + index
      edge.startFallConnectorId = Some(fallConnectorId)
      val destNode = graph.nodes(edge.endNode)
      val tunnelSg = getFloorTunnel(fallConnectorId, destNode.tunnelTheme)
      sg = sg.withGroupAttachedAutoRotate(gameCfg, conn, tunnelSg){ otherSg =>
        otherSg.allRedwallConnectors.find(_.getWallCount == 1).get
      }
    }

    // outgoing (floor)
    graph.edgesFrom(nodeId).filter(_.startConnectorId.isEmpty).zipWithIndex.foreach { case (edge, index) =>
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


  def run(gameCfg: GameConfig, random: RandomX, inputMap: DMap, writer: MapWriter): DMap = {
    val palette = ScalaMapLoader.paletteFromMap(gameCfg, inputMap)

    val nodepal = new NodePalette(gameCfg, random, palette)

    val graph = new MutableGraph()
    graph.addNode(Node2("START", nodepal.randomizeStartRoom()))
    graph.addNode(Node2("1", nodepal.bluePentagon))
    graph.addNode(Node2("2", nodepal.redGate)) // redRoom
    // graph.addNode(Node2("3", nodepal.greenRoom)) // TODO back in
    graph.addNode(Node2("3", nodepal.castleStairs))
    // graph.addNode(Node2("4", nodepal.whiteRoom)) // TODO
    graph.addNode(Node2("4", nodepal.moon3way))
    graph.addNode(Node2("5", nodepal.grayRoom))
    graph.addNode(Node2("6", nodepal.dirtRoom))
    graph.addNode(Node2("7", nodepal.blueItemRoom, NodeProperties(true)))
    graph.addNode(Node2("8", nodepal.woodRoom))
    graph.addNode(Node2("EXIT", nodepal.exitRoom))

    val edge = graph.addEdge("2", "EXIT")
    edge.startConnectorId = Some(999)

    graph.addEdge("START", "1")
    graph.addEdge("1", "2")
    graph.addEdge("2", "3")
    graph.addEdge("3", "4")
    graph.addEdge("4", "5")
    graph.addEdge("5", "6")
    graph.addEdge("6", "7")
    graph.addEdge("7", "8")
    graph.addEdge("8", "1")

    connectRandomNodes(random, graph)


    println(s"start edge count ${graph.edgeCount("START")}")


    var index = 0
    val layout = new GridLayout(BuildConstants.MapBounds, 5, 5)
    val pastedGroups = graph.nodes.map { case (nodeId, node) =>
      val sg = nodepal.getSg(graph, nodeId, node)
      val psg = writer.tryPasteInside(sg, layout.bbForIndex(index)).getOrElse(throw new SpriteLogicException("sector group too big"))
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
