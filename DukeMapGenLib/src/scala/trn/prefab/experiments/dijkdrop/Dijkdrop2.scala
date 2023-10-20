package trn.prefab.experiments.dijkdrop

import trn.{BuildConstants, UniqueTags, RandomX, HardcodedConfig, ScalaMapLoader, Sprite, Map => DMap}
import trn.prefab.{FallConnector, MapWriter, DukeConfig, GameConfig, SectorGroup, RedwallConnector, PrefabPalette, PastedSectorGroup, SpriteLogicException, Marker}
import trn.prefab.experiments.ExpUtil

import scala.collection.mutable
import scala.collection.JavaConverters._


// case class MutableNode (nodeId: String) {
// }

case class Edge(startNode: String, endNode: String) {
  var startFallConnectorId: Option[Int] = None
  var endFallConnectorId: Option[Int] = None

  def readyToLink: Boolean = startFallConnectorId.isDefined && endFallConnectorId.isDefined
}

class MutableGraph {
  val nodes = mutable.ArrayBuffer[String]()
  val edges = mutable.ArrayBuffer[Edge]()
  val edgesByStart = mutable.Map[String, mutable.ArrayBuffer[Edge]]()
  val edgesByEnd = mutable.Map[String, mutable.ArrayBuffer[Edge]]()

  def addNode(nodeId: String): Unit = nodes.append(nodeId)

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
  def isTunnelConn(conn: RedwallConnector): Boolean = {

    // TODO - pasting "loop" connectors is broken because the RedwallConnector.isMatch() fails
    // so for now I am hacking it by using 2 connectors per square.  One will be used for matching,
    // and the other will get matched based on its placement
    // conn.getWallCount == 4 && conn.totalManhattanLength() == 2048 * 4
    conn.getWallCount == 1 && conn.getConnectorId > 0 && conn.totalManhattanLength() == 2048
  }

  def getTunnelConnIds(sg: SectorGroup): Seq[Int] = {
    val tunnelConns = sg.allUnlinkedRedwallConns.filter(isTunnelConn)
    val ids = tunnelConns.map(_.getConnectorId).toSet

    if (ids.size != tunnelConns.size) {
      throw new SpriteLogicException("redwall connectors for tunnel locations are not unique", tunnelConns.map(_.getLocationXY).asJava)
    }
    ids.toSeq.sorted
  }

}

class NodePalette(gameCfg: GameConfig, random: RandomX, palette: PrefabPalette) {

  val blueRoom = palette.getSG(1)

  val outBlue = palette.getSG(100)
  val inBlue = palette.getSG(101)

  def isFallConn(sprite: Sprite): Boolean = Marker.isMarker(sprite, Marker.Lotags.FALL_CONNECTOR)

  def getFloorTunnel(connectorId: Int): SectorGroup = {
    outBlue.withModifiedSprites { sprite =>
      if(isFallConn(sprite)) {
        sprite.setHiTag(connectorId)
      }
    }
  }

  def getCeilingTunnel(connectorId: Int): SectorGroup = {
    inBlue.withModifiedSprites { sprite =>
      if(isFallConn(sprite)) {
        sprite.setHiTag(connectorId)
      }
    }
  }


  /**
    * this function need to know about incoming and outgoing nodes
    * @param node
    * @return
    */
  def getSg(graph: MutableGraph, nodeId: String): SectorGroup = {
    var sg = blueRoom

    val connIds: Seq[Int] = random.shuffle(NodePalette.getTunnelConnIds(sg)).toSeq

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
      val tunnelSg = getFloorTunnel(fallConnectorId)
      sg = sg.withGroupAttachedAutoRotate(gameCfg, conn, tunnelSg){ otherSg =>
        otherSg.allRedwallConnectors.find(_.getWallCount == 1).get
      }
    }

    // incoming (ceiling)
    graph.edgesTo(nodeId).zipWithIndex.foreach { case (edge, index) =>
      val conn = sg.getRedwallConnector(connIds(outCount + index))
      val fallConnectorId = 1000 + outCount + index
      val tunnelSg = getCeilingTunnel(fallConnectorId)
      edge.endFallConnectorId = Some(fallConnectorId)
      sg = sg.withGroupAttachedAutoRotate(gameCfg, conn, tunnelSg){ otherSg =>
        otherSg.allRedwallConnectors.find(_.getWallCount == 1).get
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

    val sg1 = palette.getSG(1)

    val graph = new MutableGraph()
    graph.addNode("1")
    graph.addNode("2")
    graph.addEdge("1", "2")
    graph.addEdge("2", "1")



    val nodepal = new NodePalette(gameCfg, random, palette)

    var index = 0
    val layout = new GridLayout(BuildConstants.MapBounds, 6, 6)
    val pastedGroups = graph.nodes.map { nodeId =>
      val sg = nodepal.getSg(graph, nodeId)
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
        println("DOING IT")
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
