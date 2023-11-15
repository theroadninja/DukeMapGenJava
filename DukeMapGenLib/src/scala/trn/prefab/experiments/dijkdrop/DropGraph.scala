package trn.prefab.experiments.dijkdrop

import trn.RandomX

import scala.collection.mutable

type RoomId = Int

case class Room(roomId: RoomId, tile: NodeTile2, nodeProps: NodeProperties) {
  require(roomId < 100)
}

case class DropNodeId(roomId: RoomId, redwallConnId: Int) {
  require(roomId < 100 && redwallConnId < 100)
}

case class DropNode(nodeId: DropNodeId, theme: Int, var edge: Option[DropEdge] = None)

case class DropEdge(
  fromNode: DropNodeId,
  toNode: DropNodeId,
  floorTheme: Int, // matches toNode
  ceilTheme: Int,  // matches fromNode
  var fromFallId: Option[Int] = None,
  var toFallId: Option[Int] = None,
)

/**
  * Next evolution of the Graph representation, after MutableGraph in DijkDrop2.
  *
  * Where MutableGraph represented every room as a node, this graph is going to
  * represent each _connector_ as a node.  Connectors in the same "room" are grouped
  * together, and the links between them are implicit.
  *
  * Ex:  Room "A" has connectors 1, 2, 3 ->   Nodes (A, 1), (A, 2), (A, 3)
  * (except rooms actually need a numeric id, to map to the fall connectors.
  *
  * NOTE:  this is still weird, because each node has exactly one edge...
  */
class DropGraph {

  val rooms = mutable.Map[Int, Room]()
  val nodes = mutable.Map[DropNodeId, DropNode]()
  val edges = mutable.ArrayBuffer[DropEdge]()

  // TODO ? val nodesByRoom = mutable.Map[Int, ...]  (map of roomId to list of nodes?)

  def getUnlinkedNodesInRoom(roomId: RoomId): Seq[DropNodeId] = {
    nodes.filter { case (nodeId, node) => nodeId.roomId == roomId && node.edge.isEmpty}.map(_._1).toSeq
  }

  def addRoom(roomId: Int, tile: NodeTile2, nodeProps: NodeProperties = NodeProperties.Default): Unit = {
    val r = Room(roomId, tile, nodeProps)
    rooms.put(r.roomId, r)
    tile.tunnelConnIds.map { tunnelConnId =>
      DropNode(DropNodeId(roomId, tunnelConnId), tile.tunnelTheme)
    }.foreach { node =>
      nodes.put(node.nodeId, node)
    }
  }

  def addEdge(from: DropNodeId, dest: DropNodeId): DropEdge = {
    val fromNode = nodes(from)
    val destNode = nodes(dest)
    require(fromNode.edge.isEmpty && destNode.edge.isEmpty)

    val edge = DropEdge(fromNode.nodeId, destNode.nodeId, floorTheme=destNode.theme, ceilTheme=fromNode.theme)
    fromNode.edge = Some(edge)
    destNode.edge = Some(edge)
    edges.append(edge)
    edge
  }

  def connectRooms(random: RandomX, from: RoomId, to: RoomId, fromRedwallConnId: Option[Int] = None): DropEdge = {
    // pick a random (unused) node for each room

    val fromNode = fromRedwallConnId.map(connId => DropNodeId(from, connId)).getOrElse(
      random.randomElement(getUnlinkedNodesInRoom(from))
    )
    val toNode = random.randomElement(getUnlinkedNodesInRoom(to))
    addEdge(fromNode, toNode)
  }

}
