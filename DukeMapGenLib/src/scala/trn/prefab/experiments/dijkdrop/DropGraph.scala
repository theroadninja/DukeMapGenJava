package trn.prefab.experiments.dijkdrop

import com.sun.jdi.InconsistentDebugInfoException
import trn.RandomX
import trn.prefab.experiments.dijkdrop.Room.RoomId

import scala.collection.mutable


object Room {
  type RoomId = Int

}

case class Room(roomId: RoomId, tile: NodeTile2, nodeProps: NodeProperties) {
  require(roomId < 100)
}

case class DropNodeId(roomId: RoomId, redwallConnId: Int) {
  require(roomId < 100 && redwallConnId < 100)

  lazy val fallConnectorId = {
    val s = "%02d".format(roomId) + "%02d".format(redwallConnId)
    s.toInt
  }
}

case class DropNode(nodeId: DropNodeId, theme: Int, var edge: Option[DropEdge] = None)

case class DropEdge(
  fromNode: DropNodeId,
  toNode: DropNodeId,
  floorTheme: Int, // matches toNode
  ceilTheme: Int,  // matches fromNode
  // var fromFallId: Option[Int] = None,  // <- these can be pulled directly off the node id...
  // var toFallId: Option[Int] = None,
) {
  def connectsToRoom(roomId: RoomId): Boolean = fromNode.roomId == roomId || toNode.roomId == roomId
}

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

  def allNodesInRoom(roomId: RoomId): Seq[DropNodeId] = {
    nodes.filter { case (nodeId, _) => nodeId.roomId == roomId}.map(_._1).toSeq
  }

  def roomsWithUnlinkedNodes: Seq[RoomId] = {
    rooms.keys.filter { roomId => getUnlinkedNodesInRoom(roomId).nonEmpty
    }.toSeq
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

  /**
    * Adds an edge from `from` to `to`.  The nodes are chosen at random, but the direction is always from->to
    * @param fromRedwallConnId - lets you specify the exact redwall conn on the `from` side (for the gate room)
    */
  def connectRooms(random: RandomX, from: RoomId, to: RoomId, fromRedwallConnId: Option[Int] = None): DropEdge = {
    // pick a random (unused) node for each room

    val fromNode = fromRedwallConnId.map(connId => DropNodeId(from, connId)).getOrElse {
      val unlinked = getUnlinkedNodesInRoom(from)
      if(unlinked.isEmpty){
        throw new Exception(s"no unlinked nodes in room ${from}")
      }
      random.randomElement(unlinked)
    }
    val toNode = random.randomElement(getUnlinkedNodesInRoom(to))
    addEdge(fromNode, toNode)
  }

  /**
    * tries to add a directed edge from A->B or B->A but will avoid adding one if one already exists
    * Note:  "A" and "B" are "rooms" which are actually groups of graph nodes.
    * @param random
    * @param roomA
    * @param roomB
    * @return
    */
  def tryAddRandomUniqueEdge(random: RandomX, roomA: RoomId, roomB: RoomId): Option[DropEdge] = {
    // def hasRoomEdge(a: RoomId, b: RoomId): Boolean = {
    //   edges.exists(e => e.fromNode.roomId == a && e.toNode.roomId == b)
    // }

    def getRoomEdges(roomA: RoomId, roomB: RoomId): Seq[DropEdge] = {
      edges.filter(e => e.connectsToRoom(roomA) && e.connectsToRoom(roomB))
    }
    if(getUnlinkedNodesInRoom(roomA).isEmpty || getUnlinkedNodesInRoom(roomB).isEmpty){
      println(s"Rooms ${roomA}, ${roomB} no unlinked nodes")
      None
    }else{
      val existing = getRoomEdges(roomA, roomB)
      existing.size match {
        case 2 => {
          println(s"Rooms ${roomA}, ${roomB} both edges already exist")
          None
        }
        case 1 => {
          if (edges.head.fromNode.roomId == roomA) {
            Some(connectRooms(random, roomB, roomA))
          } else {
            Some(connectRooms(random, roomA, roomB))
          }
        }
        case 0 => {
          if (random.nextBool()) {
            Some(connectRooms(random, roomB, roomA))
          } else {
            Some(connectRooms(random, roomA, roomB))
          }
        }
      }
    }
  }


}
