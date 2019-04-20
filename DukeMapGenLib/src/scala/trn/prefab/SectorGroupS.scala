package trn.prefab

import trn.duke.{MapErrorException, TextureList}
import trn.{DukeConstants, ISpriteFilter, PointXY, PointXYZ, Sprite, Wall, Map => DMap}
import trn.MapImplicits._

import scala.collection.JavaConverters._ // this is the good one


class CopyPasteMapBuilder(val outMap: DMap) extends MapBuilder {

}

/**
  * Set of marker + connectors used to connect a "child" sector group to a "parent" sector group.
  * @param childMarker
  * @param childConnectors
  */
case class ChildPointer(
  val childMarker: Sprite,
  val connectorId: Int,
  val childConnectors: Seq[RedwallConnector],

  // NOTE: these references becone invalid after the sector merge
  //val waterConnectors: Seq[TeleportConnector],
  //val elevatorConnectors: Seq[ElevatorConnector]
)
{
  def sectorId: Int = childMarker.getSectorId
  def parentGroupId: Int = childMarker.getHiTag

  def connectorsJava: java.util.List[RedwallConnector] = childConnectors.asJava
}


trait ConnectorCollection {
  def connectors: java.util.List[Connector]
  def map: DMap

  final def allConnectorIds(): java.util.Set[Integer] = {
    connectors.asScala.map(_.connectorId).filter(_ > 0).map(_.asInstanceOf[Integer]).toSet.asJava
  }

  final def getConnector(connectorId: Int): Connector = {
    if(connectorId < 0) throw new IllegalArgumentException
    connectors.asScala.find(_.connectorId == connectorId) match {
      case Some(conn) => conn
      case None => throw new NoSuchElementException
    }
  }

  final def getRedwallConnectorsById(connectorId: Int): Seq[RedwallConnector] = {
    if(connectorId < 0) throw new IllegalArgumentException
    val c = connectors.asScala.filter(c => ConnectorType.isRedwallType(c.getConnectorType) && c.connectorId == connectorId)
    c.map(_.asInstanceOf[RedwallConnector])
  }

  final def getElevatorConnectorsById(connectorId: Int): Seq[ElevatorConnector] = {
    if(connectorId < 0) throw new IllegalArgumentException
    connectors.asScala.filter(_.getConnectorType == ConnectorType.ELEVATOR).map(_.asInstanceOf[ElevatorConnector])
  }

  final def getChildPointer(): ChildPointer = {
    val sprites: Seq[Sprite] = map.allSprites.filter(s => s.getTexture == PrefabUtils.MARKER_SPRITE_TEX && s.getLotag == PrefabUtils.MarkerSpriteLoTags.REDWALL_CHILD)
    if(sprites.size != 1) throw new SpriteLogicException(s"Wrong number of child marker sprites (${sprites.size})")
    val marker: Sprite = sprites(0)

    val conns = connectors.asScala.filter(c => c.getSectorId == marker.getSectorId && ConnectorType.isRedwallType(c.getConnectorType))
    if(conns.size != 1) throw new SpriteLogicException(s"There must be exactly 1 redwall connector in sector with child marker, but there are ${conns.size}")
    val mainConn = conns(0)
    if(mainConn.connectorId < 1) throw new SpriteLogicException(s"Connector for child pointer must have ID > 0")

    val allConns = connectors.asScala.filter(c => c.connectorId == mainConn.getConnectorId)

    val groupedConns = allConns.groupBy(c => {
      if(ConnectorType.isRedwallType(c.getConnectorType)){
        20
      }else if(c.getConnectorType == ConnectorType.ELEVATOR){
        ConnectorType.ELEVATOR
      }else if(c.getConnectorType == ConnectorType.TELEPORTER && (c.asInstanceOf[TeleportConnector]).isWater){
        ConnectorType.TELEPORTER
      }else{
        throw new SpriteLogicException(s"invalid child connector (type ${c.getConnectorType}) in child sector group")
      }
    })
    ChildPointer(
      marker,
      mainConn.connectorId,
      groupedConns.getOrElse(20, Seq()).map(_.asInstanceOf[RedwallConnector]),
      //groupedConns.getOrElse(ConnectorType.TELEPORTER, Seq()).map(_.asInstanceOf[TeleportConnector]),
      //groupedConns.getOrElse(ConnectorType.ELEVATOR, Seq()).map(_.asInstanceOf[ElevatorConnector])
    )

    // if(allConns.find(c => !ConnectorType.isRedwallType(c.getConnectorType)).nonEmpty){
    //   throw new SpriteLogicException(s"child sector cannot connect with non redwall connector (id=${mainConn.getConnectorId}")
    // }else{
    //   ChildPointer(marker, mainConn.connectorId, allConns.map(_.asInstanceOf[RedwallConnector]))
    // }
  }
}


class SectorGroupS(val map: DMap, val sectorGroupId: Int) extends ConnectorCollection {
  val connectors: java.util.List[Connector] = new java.util.ArrayList[Connector]();

  def copy(): SectorGroup = {
    new SectorGroup(map.copy, this.sectorGroupId);
  }
  /**
    * @return a copy of this sector group, flipped about the X axis
    */
  def flippedX(x: Int): SectorGroup = {
    new SectorGroup(map.flippedX(x), this.sectorGroupId)
  }

  def flippedX(): SectorGroup = flippedX(getAnchor.x)



  def flippedY(y: Int): SectorGroup = {
    new SectorGroup(map.flippedY(y), this.sectorGroupId)
  }

  def flippedY(): SectorGroup = flippedY(getAnchor.y)

  def rotateAroundCW(anchor: PointXY): SectorGroup = {
    new SectorGroup(map.rotatedCW(anchor), this.sectorGroupId)
  }

  def rotateAroundCW(anchor: PointXYZ): SectorGroup = rotateAroundCW(anchor.asXY)

  @throws(classOf[MapErrorException])
  protected def updateConnectors(): Unit = ???



  // TODO - move to trait
  def getRedwallConnector(connectorType: Int, allowMoreThanOne: Boolean = false): RedwallConnector = {
    if(! ConnectorType.isRedwallType(connectorType)){
      throw new IllegalArgumentException(s"not a redwall connector type: ${connectorType}")
    }
    val matching = connectors.asScala.filter(c => c.getConnectorType == connectorType)
    matching.size match {
      case i: Int if i < 1 => throw new NoSuchElementException(s"no connector with type ${connectorType} exists")
      case i: Int if i == 1 || allowMoreThanOne => matching.head.asInstanceOf[RedwallConnector]
      case _ => throw new SpriteLogicException(s"more than one connector with type ${connectorType} found")
    }
  }

  // TODO - move to trait
  def getRedwallConnectors(connectorType: Int): Seq[RedwallConnector] = {
    if(! ConnectorType.isRedwallType(connectorType)){
      throw new IllegalArgumentException(s"not a redwall connector type: ${connectorType}")
    }
    connectors.asScala.filter(c => c.getConnectorType == connectorType).map(_.asInstanceOf[RedwallConnector])
  }

  def getTeleportConnectors(): Seq[TeleportConnector] = {
    connectors.asScala.filter(c => c.getConnectorType == ConnectorType.TELEPORTER).map(_.asInstanceOf[TeleportConnector])
  }

  def getElevatorConnectors(): Seq[ElevatorConnector] = {
    connectors.asScala.filter(c => c.getConnectorType == ConnectorType.ELEVATOR).map(_.asInstanceOf[ElevatorConnector])
  }

  def connectedTo(joinType: RedwallJoinType, group2: SectorGroup): SectorGroup = {
    val conn1 = getRedwallConnector(joinType.connectorType1)
    val conn2 = group2.getRedwallConnector(joinType.connectorType2, false)
    connectedTo(conn1, group2, conn2)
  }

  // Merging
  //  - what do we do about two anchors?
  //  - can we remove the redwall connectors?
  // TODO - right now, the sector copy code ONLY copies sectors linked with red walls.  Two unconnected
  // sectors will not both be copied (the bug isnt here, but in the main copy code ...
  def connectedTo(conn1: RedwallConnector, group2: SectorGroup, conn2: RedwallConnector): SectorGroup = {
    if(conn1 == conn2) throw new IllegalArgumentException("same connection object passed") // sanity check

    val result = this.copy()

    val anchorSprite = result.getAnchorSprite
    val removeSecondAnchor = anchorSprite.nonEmpty && group2.getAnchorSprite.nonEmpty

    val tmpBuilder = new CopyPasteMapBuilder(result.map)
    val cdelta = conn2.getTransformTo(conn1)
    val (_, idmap) = tmpBuilder.pasteSectorGroup2(group2, cdelta)
    val pastedConn2 = conn2.translateIds(idmap, cdelta)

    // TODO - link redwalls  ( TODO - make this a member of the builder? )
    //PrefabUtils.joinWalls(result.map, conn1, pastedConn2)
    conn1.linkConnectors(result.map, pastedConn2)

    // CLEANUP:  remove any other anchor sprite
    if(removeSecondAnchor){
      tmpBuilder.outMap.deleteSprites(new ISpriteFilter {
        override def matches(s: Sprite): Boolean = {
          tmpBuilder.isAnchor(s) && s.getLocation != anchorSprite.get.getLocation
        }
      })
      // for(i <- 0 until tmpBuilder.outMap.getSpriteCount){
      //   val s: Sprite = tmpBuilder.outMap.getSprite(i)
      //   if(tmpBuilder.isAnchor(s) && s.getLocation != anchorSprite.get){
      //     tmpBuilder.outMap.deleteSprite(i) // it is the other anchor, delete it
      //   }
      // }
    }

    // DELETE USED CONNECTORS
    conn1.removeConnector(tmpBuilder.outMap)
    pastedConn2.removeConnector(tmpBuilder.outMap)


    if(result.getSectorCount != this.map.getSectorCount + group2.getSectorCount){
      throw new SpriteLogicException()
    }

    result.updateConnectors()
    result
  }

  def connectedToChild(childPtr: ChildPointer, child: SectorGroup, tagGenerator: TagGenerator): SectorGroup = {
    val c1 = getRedwallConnectorsById(childPtr.connectorId)
    val c2 = child.getRedwallConnectorsById(childPtr.connectorId)
    if(this.sectorGroupId != childPtr.parentGroupId || c1.size != c2.size) throw new IllegalArgumentException

    if(c1.size > 1) throw new RuntimeException("not implemented yet")

    val result = connectedTo(c1(0), child, c2(0))

    // TODO - but now all the connectors are fucked up...

    val conns = result.getTeleportConnectors().filter(c => c.isWater && !c.isLinked(result.map) && c.getConnectorId == childPtr.connectorId)
    MapBuilder.linkAllWater(result, conns, tagGenerator)

    // find elevators with matching connector Ids
    val elevatorConns = result.getElevatorConnectorsById(childPtr.connectorId)
    if(elevatorConns.size == 2){
      val sorted = elevatorConns.sortBy(ElevatorConnector.sortKey(_, result.map))
      ElevatorConnector.linkElevators(sorted(0), result, sorted(1), result, tagGenerator.nextUniqueHiTag(), false)
    }else if(elevatorConns.size != 0){
      throw new RuntimeException("not implemented yet") // need to auto detect which elevators match ...
    }

    result
  }



  private def wallSeq(): Seq[Wall] = {
    val walls = Seq.newBuilder[Wall]
    for(i <- 0 until map.getWallCount){
      walls += map.getWall(i)
    }
    walls.result
  }

  private def bbDimension(values: Seq[Int]): Int = values.max - values.min
  def bbHeight: Int = bbDimension(wallSeq.map(_.getY))
  def bbWidth: Int = bbDimension(wallSeq.map(_.getX))

  /**
    * Returns the "top left" corner of the bounding box, to aid in placing the group on a map.
    * In duke build files, the x and y axis are like this:
    *
    *   -  <-- X --> +
    *   |
    *   Y
    *   |
    *   +
    *
    * @return
    */
  def bbTopLeft: (Int, Int) = {
    val x = wallSeq.map(_.getX)
    val y = wallSeq.map(_.getY)
    (x.min, y.min)
  }

  def boundingBox: BoundingBox = {
    val walls = wallSeq
    val w = walls(0)
    walls.foldLeft(BoundingBox(w.getX, w.getY, w.getX, w.getY)){ (acc, wall) =>
      acc.add(wall.getX, wall.getY)
    }
  }

  def hasMarker(lotag: Int): Boolean = {
    for(i <- 0 until map.getSpriteCount){
      val sprite = map.getSprite(i)
      if(sprite.getTexture == PrefabUtils.MARKER_SPRITE_TEX && sprite.getLotag == lotag){
        return true
      }
    }
    return false
  }

  def containsSprite(f: (Sprite) => Boolean): Boolean = {
    for(i <- 0 until map.getSpriteCount){
      if(f(map.getSprite(i))){
        return true
      }
    }
    return false
  }

  def sprites: Seq[Sprite] = map.allSprites

  def hasPlayerStart: Boolean = hasMarker(PrefabUtils.MarkerSpriteLoTags.PLAYER_START)

  def hasEndGame: Boolean = containsSprite{ s =>
    s.getTexture == DukeConstants.TEXTURES.NUKE_BUTTON && s.getLotag == DukeConstants.LOTAGS.NUKE_BUTTON_END_LEVEL
  }

  def getAnchor: PointXYZ = getAnchorSprite.getOrElse(
    throw new SpriteLogicException("no anchor sprite")
  ).getLocation

  def getAnchorSprite: Option[Sprite] = sprites.find(MapBuilder.isAnchorSprite)

}
