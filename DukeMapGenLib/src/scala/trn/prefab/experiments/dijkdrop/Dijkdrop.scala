package trn.prefab.experiments.dijkdrop

import trn.prefab.experiments.ExpUtil
import trn.prefab.{FallConnector, BoundingBox, MapWriter, DukeConfig, GameConfig, SectorGroup, Connector, ConnectorType, PrefabPalette, PastedSectorGroup, SpriteLogicException}
import trn.{BuildConstants, UniqueTags, RandomX, HardcodedConfig, ScalaMapLoader, Map => DMap}

case class NodeConnectorId(
  nodeId: Int,
  connId: Int, // id of the fall connector
  // incoming: Boolean,
)

case class Node(
  nodeId: Int,
  nodeTile: NodeTile,
) {
  def sg: SectorGroup = nodeTile.sg

  lazy val incoming: Seq[NodeConnectorId] = nodeTile.incoming.map(c => NodeConnectorId(nodeId, c))
  lazy val outgoing: Seq[NodeConnectorId] = nodeTile.outgoing.map(c => NodeConnectorId(nodeId, c))
}

case class NodeTile(
  sg: SectorGroup,
  incoming: Seq[Int], // connector ids
  outgoing: Seq[Int]
) {

}

object NodeTile {

  def apply(sg: SectorGroup): NodeTile = {
    val conns = sg.allConnectors.filter(c => FallConnector.isFallConn(c)).map(_.asInstanceOf[FallConnector])
    val (floorConns, ceilConns) = conns.partition(_.closerToFloor())
    NodeTile(sg, ceilConns.map(_.getConnectorId), floorConns.map(_.getConnectorId))
  }
}

case class DropPalette(palette: PrefabPalette) {
  val center = NodeTile(palette.getSG(1))
  val armory = NodeTile(palette.getSG(2))

  // includes pre-k and pre-g
  val normals = Seq(NodeTile(palette.getSG(3)))

  val gate = NodeTile(palette.getSG(4))
}

case class FallLink(
  nodeConnA: NodeConnectorId,
  nodeConnB: NodeConnectorId,
) {
  lazy val toSeq: Seq[NodeConnectorId] = Seq(nodeConnA, nodeConnB)
}

object FallLink {
  def apply(t: (NodeConnectorId, NodeConnectorId)): FallLink = FallLink(t._1, t._2)
}

object Dijkdrop {
  val Filename = "dijkdrop.map"

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

  def getFallConn(connectors: Iterable[Connector], connId: Int): FallConnector = {
    connectors.find(c => FallConnector.isFallConn(c) && c.getConnectorId == connId).get.asInstanceOf[FallConnector]
  }

  def nextTag(gameCfg: GameConfig, map: DMap): Int = {
    val usedTags = UniqueTags.usedUniqueTags(gameCfg, map)
    (Set(0) ++ usedTags).max + 1
  }

  def linkFallConns(gameCfg: GameConfig, pastedGroups: Map[Int, PastedSectorGroup], link: FallLink, channel: Int): Unit = {
    val psgA = pastedGroups(link.nodeConnA.nodeId)
    val connA = getFallConn(psgA.allConnsInPsg, link.nodeConnA.connId)
    val psgB = pastedGroups(link.nodeConnB.nodeId)
    val connB = getFallConn(psgB.allConnsInPsg, link.nodeConnB.connId)

    connA.replaceMarkerSprite(psgA.map, channel)
    connB.replaceMarkerSprite(psgB.map, channel)
  }

  def run(gameCfg: GameConfig, random: RandomX, inputMap: DMap, writer: MapWriter): DMap = {
    val palette = DropPalette(ScalaMapLoader.paletteFromMap(gameCfg, inputMap))

    val layout = new GridLayout(BuildConstants.MapBounds, 6, 6)

    val Center = 1
    val Armory = 2
    val PreGate = 3
    val Gate = 4


    val nodes: Map[Int, Node] = Seq(
      Node(Center, palette.center),
      Node(Armory, palette.armory),
      Node(PreGate, palette.normals(0)),
      Node(Gate, palette.gate),
    ).map(n => n.nodeId -> n).toMap

    // TODO not here yet:
    // val fixedLinks = Seq(
    //   (Center, Armory),
    // )
    val fixedLinks = Seq(
      FallLink(NodeConnectorId(Center, 1), NodeConnectorId(Armory, 2)),
      FallLink(NodeConnectorId(Center, 2), NodeConnectorId(PreGate, 2)), // center to normal
      FallLink(NodeConnectorId(Center, 3), NodeConnectorId(Gate, 1)),
      FallLink(NodeConnectorId(PreGate, 5), NodeConnectorId(Gate, 2)),
      FallLink(NodeConnectorId(Gate, 4), NodeConnectorId(Center, 7))
    )

    val taken = fixedLinks.flatMap(_.toSeq).toSet

    val remainingCenterIncoming = nodes(Center).incoming.filterNot(taken.contains)
    println(s"center incoming: ${nodes(Center).incoming} remaining: ${remainingCenterIncoming.size}")
    val outgoing = nodes.values.flatMap(node => node.outgoing).filterNot(taken.contains)
    val outgoingToMatch = random.shuffle(outgoing).toSeq.take(remainingCenterIncoming.size)

    val routedToCenter = remainingCenterIncoming.zip(outgoingToMatch).map(t => FallLink(t))

    val taken2 = taken ++ routedToCenter.flatMap(_.toSeq)


    //
    //  RENDERING
    //

    val pastedGroups = nodes.map {
      case (index, node) => {
        val bb = layout.bbForIndex(index)
        val psg = writer.tryPasteInside(node.sg, bb).get
        index -> psg
      }
    }.toMap

    (fixedLinks ++ routedToCenter).foreach { link =>
      linkFallConns(
        gameCfg,
        pastedGroups, link, nextTag(gameCfg, writer.getMap)
      )
    }

    ExpUtil.finish(writer)
    writer.outMap
  }
}
